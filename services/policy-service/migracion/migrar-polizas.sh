#!/usr/bin/env sh
# HISTORICO (retiro del monolito, 2026-09-28): este script ya se ejecuto durante el corte y no se
# puede volver a usar: lee el MongoDB del monolito (andina-clean-mongodb), que ya no esta en el
# Compose. Se conserva como evidencia de la migracion. Para inspeccionar aquellos datos, restaurar
# respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz (ver k_IMPACTO-EN-EL-MONOLITO.md).
#
# Pasos 6.2 y 6.7 de la ruta:
#   1. Copia "polizas" y "propuestas_renovacion" del MongoDB del backend a policy_db y compara las
#      copias (cantidad y huella SHA-256).
#   2. Carga inicial de accepted_quotes (cotizaciones ACEPTADA del backend) y de claim_ref (un
#      documento por siniestro: abierto y responsable), y marca claim_ref como sincronizada: sin esa
#      marca policy-service no evalua renovaciones (paso 6.5).
#
# Uso (desde cualquier carpeta, con el stack del monolito levantado):
#   sh services/policy-service/migracion/migrar-polizas.sh
#
# Se puede repetir mientras el monolito sea la fuente. Despues del corte no: las proyecciones las
# mantienen quote.accepted.v1 y claim.*. La base del backend solo se lee.
set -eu

ORIGEN="${ORIGEN_CONTENEDOR:-andina-clean-mongodb}"
ORIGEN_DB="${ORIGEN_DB:-andina_seguros_clean}"
DESTINO="${DESTINO_CONTENEDOR:-policy-mongodb}"
ROOT_PASS="${POLICY_MONGO_ROOT_PASSWORD:-policy-root-local}"
ORIGEN_ROOT_PASS="${BACKEND_MONGO_ROOT_PASSWORD:-backend-root-local}"

# --noIndexRestore: no se copian los indices del monolito (su indice de cotizacionId no es parcial
# y chocaria con las polizas renovadas). Los de policy-service se recrean en el paso 2.
copiar() {
  docker exec "$ORIGEN" mongodump --quiet --db "$ORIGEN_DB" --collection "$1" --archive \
      --username root --password "$ORIGEN_ROOT_PASS" --authenticationDatabase admin \
    | docker exec -i "$DESTINO" mongorestore --quiet --archive --drop --noIndexRestore \
        --username root --password "$ROOT_PASS" --authenticationDatabase admin \
        --nsFrom "${ORIGEN_DB}.$1" --nsTo "policy_db.$2"
}

echo "1/3 Copiando polizas y propuestas_renovacion"
copiar polizas polizas
copiar propuestas_renovacion propuestas_renovacion

echo "2/3 Carga inicial de accepted_quotes y claim_ref"
copiar cotizaciones carga_cotizaciones_tmp
copiar siniestros carga_siniestros_tmp
docker exec "$DESTINO" mongosh --quiet --username root --password "$ROOT_PASS" \
  --authenticationDatabase admin --eval '
    const db2 = db.getSiblingDB("policy_db");
    // --drop borro los indices: se recrean aqui mismo (mismos nombres que usa policy-service), asi
    // la proteccion contra la doble emision no depende de reiniciar el servicio.
    db2.polizas.createIndex({ cotizacionId: 1 }, { name: "cotizacionId_unico", unique: true,
                             partialFilterExpression: { cotizacionId: { $type: "string" } } });
    db2.polizas.createIndex({ numero: 1 }, { name: "numero", unique: true });
    db2.polizas.createIndex({ renovacionOrigenId: 1 }, { name: "renovacionOrigenId", unique: true, sparse: true });
    db2.polizas.createIndex({ clienteId: 1 }, { name: "clienteId" });
    db2.propuestas_renovacion.createIndex({ polizaOrigenId: 1 }, { name: "polizaOrigenId" });
    db2.polizas.updateMany({}, { $unset: { _class: "" } });
    db2.propuestas_renovacion.updateMany({}, { $unset: { _class: "" } });
    db2.carga_cotizaciones_tmp.aggregate([
      { $match: { estado: "ACEPTADA" } },
      { $project: { _id: 1, numero: 1, clienteId: 1, vehiculoId: 1, prima: 1, moneda: 1,
                    expira: "$fechaExpiracion", origen: { $literal: "CARGA_INICIAL" }, recibidaEn: "$$NOW" } },
      { $merge: { into: "accepted_quotes", on: "_id", whenMatched: "keepExisting", whenNotMatched: "insert" } }
    ]);
    db2.carga_siniestros_tmp.aggregate([
      { $project: { _id: 1, polizaId: 1, responsabilidadAsegurado: 1,
                    abierto: { $not: [ { $in: ["$estado", ["LIQUIDADO", "RECHAZADO"]] } ] },
                    version: { $ifNull: ["$version", 1] }, actualizadoEn: "$$NOW" } },
      { $merge: { into: "claim_ref", on: "_id", whenNotMatched: "insert",
                  whenMatched: [ { $replaceWith: { $cond: [ { $gte: ["$$new.version", "$version"] }, "$$new", "$$ROOT" ] } } ] } }
    ]);
    db2.carga_cotizaciones_tmp.drop();
    db2.carga_siniestros_tmp.drop();
    db2.sync_state.updateOne({ _id: "claim_ref" }, { $set: { cargadaEn: new Date(), origen: "CARGA_INICIAL" } }, { upsert: true });
    print("   accepted_quotes: " + db2.accepted_quotes.countDocuments() + ", claim_ref: " + db2.claim_ref.countDocuments()
          + " (abiertos: " + db2.claim_ref.countDocuments({ abierto: true }) + ")");'

echo "3/3 Comparando polizas y propuestas_renovacion"
HUELLA='for (const c of ["polizas", "propuestas_renovacion"]) {
  const docs = db.getSiblingDB(DB)[c].find({}, {_class: 0}).sort({_id: 1}).toArray();
  print(c + " " + docs.length + " " + require("crypto").createHash("sha256").update(EJSON.stringify(docs, {relaxed: false})).digest("hex"));
}'
EN_ORIGEN=$(docker exec "$ORIGEN" mongosh --quiet --username root --password "$ORIGEN_ROOT_PASS" \
  --authenticationDatabase admin --eval "const DB='${ORIGEN_DB}'; ${HUELLA}")
EN_DESTINO=$(docker exec "$DESTINO" mongosh --quiet --username root --password "$ROOT_PASS" \
  --authenticationDatabase admin --eval "const DB='policy_db'; ${HUELLA}")
echo "backend:"
echo "$EN_ORIGEN" | sed 's/^/   /'
echo "policy-service:"
echo "$EN_DESTINO" | sed 's/^/   /'

if [ "$EN_ORIGEN" != "$EN_DESTINO" ]; then
  echo "ERROR: las copias no coinciden; no hacer el corte de rutas." >&2
  exit 1
fi
echo "OK: mismas polizas y renovaciones; proyecciones cargadas."
