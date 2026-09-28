#!/usr/bin/env sh
# HISTORICO (retiro del monolito, 2026-09-28): este script ya se ejecuto durante el corte y no se
# puede volver a usar: lee el MongoDB del monolito (andina-clean-mongodb), que ya no esta en el
# Compose. Se conserva como evidencia de la migracion. Para inspeccionar aquellos datos, restaurar
# respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz (ver k_IMPACTO-EN-EL-MONOLITO.md).
#
# Pasos 4.3 y 4.6 de la ruta:
#   1. Copia la coleccion "siniestros" del MongoDB del backend a claims_db y compara las copias.
#   2. Carga inicial de la proyeccion policy_ref: por cada poliza del backend guarda solo lo que
#      claims necesita (id, cliente, numero y estado). Despues la mantiene policy.issued.v1.
#
# Uso (desde cualquier carpeta, con el stack del monolito levantado):
#   sh services/claims-service/migracion/migrar-siniestros.sh
#
# Se puede repetir: reemplaza los siniestros (--drop) y actualiza policy_ref con el estado actual
# de las polizas. Mientras el backend no publique policy.renewed/expired/cancelled (lo hara
# policy-service en la fase 6), repetir este script es la forma de resincronizar policy_ref con los
# cambios de estado. La base del backend solo se lee.
set -eu

ORIGEN="${ORIGEN_CONTENEDOR:-andina-clean-mongodb}"
ORIGEN_DB="${ORIGEN_DB:-andina_seguros_clean}"
DESTINO="${DESTINO_CONTENEDOR:-claims-mongodb}"
ROOT_PASS="${CLAIMS_MONGO_ROOT_PASSWORD:-claims-root-local}"
ORIGEN_ROOT_PASS="${BACKEND_MONGO_ROOT_PASSWORD:-backend-root-local}"

copiar() {
  docker exec "$ORIGEN" mongodump --quiet --db "$ORIGEN_DB" --collection "$1" --archive \
      --username root --password "$ORIGEN_ROOT_PASS" --authenticationDatabase admin \
    | docker exec -i "$DESTINO" mongorestore --quiet --archive --drop \
        --username root --password "$ROOT_PASS" --authenticationDatabase admin \
        --nsFrom "${ORIGEN_DB}.$1" --nsTo "claims_db.$2"
}

echo "1/3 Copiando ${ORIGEN_DB}.siniestros -> claims_db.siniestros"
copiar siniestros siniestros

echo "2/3 Carga inicial de policy_ref desde ${ORIGEN_DB}.polizas"
# Las polizas completas pasan por una coleccion temporal y solo se guardan los cuatro campos.
copiar polizas carga_polizas_tmp
docker exec "$DESTINO" mongosh --quiet --username root --password "$ROOT_PASS" \
  --authenticationDatabase admin --eval '
    const db2 = db.getSiblingDB("claims_db");
    db2.siniestros.updateMany({}, { $unset: { _class: "" } });
    db2.carga_polizas_tmp.aggregate([
      { $project: { _id: 1, clienteId: 1, numero: 1, estado: 1,
                    origen: { $literal: "CARGA_INICIAL" }, actualizadoEn: "$$NOW" } },
      { $merge: { into: "policy_ref", on: "_id", whenMatched: "merge", whenNotMatched: "insert" } }
    ]);
    db2.carga_polizas_tmp.drop();
    print("   policy_ref: " + db2.policy_ref.countDocuments() + " polizas");'

echo "3/3 Comparando siniestros"
HUELLA='const docs = db.getSiblingDB(DB).siniestros.find({}, {_class: 0}).sort({_id: 1}).toArray();
print(docs.length + " " + require("crypto").createHash("sha256").update(EJSON.stringify(docs, {relaxed: false})).digest("hex"))'
EN_ORIGEN=$(docker exec "$ORIGEN" mongosh --quiet --username root --password "$ORIGEN_ROOT_PASS" \
  --authenticationDatabase admin --eval "const DB='${ORIGEN_DB}'; ${HUELLA}")
EN_DESTINO=$(docker exec "$DESTINO" mongosh --quiet --username root --password "$ROOT_PASS" \
  --authenticationDatabase admin --eval "const DB='claims_db'; ${HUELLA}")
POLIZAS=$(docker exec "$ORIGEN" mongosh --quiet --username root --password "$ORIGEN_ROOT_PASS" \
  --authenticationDatabase admin --eval "db.getSiblingDB('${ORIGEN_DB}').polizas.countDocuments()")
echo "   backend: ${EN_ORIGEN} (polizas: ${POLIZAS})"
echo "   claims : ${EN_DESTINO}"

if [ "$EN_ORIGEN" != "$EN_DESTINO" ]; then
  echo "ERROR: las copias no coinciden; no hacer el corte de rutas." >&2
  exit 1
fi
echo "OK: mismos siniestros y mismo contenido; policy_ref cargada."
