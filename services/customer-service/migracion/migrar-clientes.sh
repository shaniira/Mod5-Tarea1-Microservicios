#!/usr/bin/env sh
# HISTORICO (retiro del monolito, 2026-09-28): este script ya se ejecuto durante el corte y no se
# puede volver a usar: lee el MongoDB del monolito (andina-clean-mongodb), que ya no esta en el
# Compose. Se conserva como evidencia de la migracion. Para inspeccionar aquellos datos, restaurar
# respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz (ver k_IMPACTO-EN-EL-MONOLITO.md).
#
# Paso 3.4 de la ruta: copia las colecciones "clientes" y "vehiculos" del MongoDB del backend a
# customer_db y compara las dos copias (cantidad y huella SHA-256 de cada coleccion).
#
# Uso (desde cualquier carpeta, con el stack del monolito levantado):
#   sh services/customer-service/migracion/migrar-clientes.sh
#
# Se puede repetir: cada ejecucion reemplaza la copia (--drop). La base del backend solo se lee:
# es la fuente de verdad mientras /api/clientes siga yendo al monolito, y el plan de reversa despues
# del corte. Repetirla justo antes del corte deja customer_db al dia.
set -eu

ORIGEN="${ORIGEN_CONTENEDOR:-andina-clean-mongodb}"
ORIGEN_DB="${ORIGEN_DB:-andina_seguros_clean}"
DESTINO="${DESTINO_CONTENEDOR:-customer-mongodb}"
ROOT_PASS="${CUSTOMER_MONGO_ROOT_PASSWORD:-customer-root-local}"
ORIGEN_ROOT_PASS="${BACKEND_MONGO_ROOT_PASSWORD:-backend-root-local}"

for COLECCION in clientes vehiculos; do
  echo "Copiando ${ORIGEN_DB}.${COLECCION} -> customer_db.${COLECCION}"
  docker exec "$ORIGEN" mongodump --quiet --db "$ORIGEN_DB" --collection "$COLECCION" --archive \
      --username root --password "$ORIGEN_ROOT_PASS" --authenticationDatabase admin \
    | docker exec -i "$DESTINO" mongorestore --quiet --archive --drop \
        --username root --password "$ROOT_PASS" --authenticationDatabase admin \
        --nsFrom "${ORIGEN_DB}.${COLECCION}" --nsTo "customer_db.${COLECCION}"
done

# _class guarda el nombre de la clase Java del monolito; customer-service tiene otras clases y no lo
# necesita (lee por coleccion).
docker exec "$DESTINO" mongosh --quiet --username root --password "$ROOT_PASS" \
  --authenticationDatabase admin --eval '
    const db2 = db.getSiblingDB("customer_db");
    db2.clientes.updateMany({}, { $unset: { _class: "" } });
    db2.vehiculos.updateMany({}, { $unset: { _class: "" } });' > /dev/null

# Huella de cada coleccion: documentos ordenados por _id (sin _class), en EJSON canonico.
HUELLA='for (const c of ["clientes", "vehiculos"]) {
  const docs = db.getSiblingDB(DB)[c].find({}, {_class: 0}).sort({_id: 1}).toArray();
  print(c + " " + docs.length + " " + require("crypto").createHash("sha256").update(EJSON.stringify(docs, {relaxed: false})).digest("hex"));
}'

echo "Comparando"
EN_ORIGEN=$(docker exec "$ORIGEN" mongosh --quiet --username root --password "$ORIGEN_ROOT_PASS" \
  --authenticationDatabase admin --eval "const DB='${ORIGEN_DB}'; ${HUELLA}")
EN_DESTINO=$(docker exec "$DESTINO" mongosh --quiet --username root --password "$ROOT_PASS" \
  --authenticationDatabase admin --eval "const DB='customer_db'; ${HUELLA}")
echo "backend:"
echo "$EN_ORIGEN" | sed 's/^/   /'
echo "customer-service:"
echo "$EN_DESTINO" | sed 's/^/   /'

if [ "$EN_ORIGEN" != "$EN_DESTINO" ]; then
  echo "ERROR: las copias no coinciden; no hacer el corte de rutas." >&2
  exit 1
fi
echo "OK: mismos clientes y vehiculos, mismo contenido."
