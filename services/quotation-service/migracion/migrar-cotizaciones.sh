#!/usr/bin/env sh
# HISTORICO (retiro del monolito, 2026-09-28): este script ya se ejecuto durante el corte y no se
# puede volver a usar: lee el MongoDB del monolito (andina-clean-mongodb), que ya no esta en el
# Compose. Se conserva como evidencia de la migracion. Para inspeccionar aquellos datos, restaurar
# respaldos/monolito-andina_seguros_clean-2026-09-27.archive.gz (ver k_IMPACTO-EN-EL-MONOLITO.md).
#
# Pasos 5.2 y 5.5 de la ruta:
#   1. Copia "tablas_tarifarias" y "cotizaciones" del MongoDB del backend a quotation_db y compara
#      las copias (cantidad y huella SHA-256).
#   2. Carga inicial de las proyecciones customer_ref y vehicle_ref: por cada cliente y vehiculo del
#      backend guarda solo lo que necesita la tarificacion (fecha de nacimiento; dueno, tipo, uso y
#      anio), con su version. Despues las mantienen customer.* y vehicle.registered.v1.
#
# Uso (desde cualquier carpeta, con el stack del monolito levantado):
#   sh services/quotation-service/migracion/migrar-cotizaciones.sh
#
# Se puede repetir: reemplaza tarifas y cotizaciones (--drop) y actualiza las proyecciones. La base
# del backend solo se lee.
set -eu

ORIGEN="${ORIGEN_CONTENEDOR:-andina-clean-mongodb}"
ORIGEN_DB="${ORIGEN_DB:-andina_seguros_clean}"
DESTINO="${DESTINO_CONTENEDOR:-quotation-mongodb}"
ROOT_PASS="${QUOTATION_MONGO_ROOT_PASSWORD:-quotation-root-local}"
ORIGEN_ROOT_PASS="${BACKEND_MONGO_ROOT_PASSWORD:-backend-root-local}"

copiar() {
  docker exec "$ORIGEN" mongodump --quiet --db "$ORIGEN_DB" --collection "$1" --archive \
      --username root --password "$ORIGEN_ROOT_PASS" --authenticationDatabase admin \
    | docker exec -i "$DESTINO" mongorestore --quiet --archive --drop \
        --username root --password "$ROOT_PASS" --authenticationDatabase admin \
        --nsFrom "${ORIGEN_DB}.$1" --nsTo "quotation_db.$2"
}

echo "1/3 Copiando tablas_tarifarias y cotizaciones"
copiar tablas_tarifarias tablas_tarifarias
copiar cotizaciones cotizaciones

echo "2/3 Carga inicial de customer_ref y vehicle_ref"
copiar clientes carga_clientes_tmp
copiar vehiculos carga_vehiculos_tmp
docker exec "$DESTINO" mongosh --quiet --username root --password "$ROOT_PASS" \
  --authenticationDatabase admin --eval '
    const db2 = db.getSiblingDB("quotation_db");
    db2.tablas_tarifarias.updateMany({}, { $unset: { _class: "" } });
    db2.cotizaciones.updateMany({}, { $unset: { _class: "" } });
    // Solo reemplaza una referencia si la version del backend es mayor o igual.
    db2.carga_clientes_tmp.aggregate([
      { $project: { _id: 1, fechaNacimiento: 1, activo: 1,
                    version: { $ifNull: ["$version", 1] }, actualizadoEn: "$$NOW" } },
      { $merge: { into: "customer_ref", on: "_id", whenNotMatched: "insert",
                  whenMatched: [ { $replaceWith: { $cond: [ { $gte: ["$$new.version", "$version"] }, "$$new", "$$ROOT" ] } } ] } }
    ]);
    db2.carga_vehiculos_tmp.aggregate([
      { $project: { _id: 1, clienteId: 1, tipo: 1, uso: 1, anioFabricacion: 1,
                    version: { $literal: 1 }, actualizadoEn: "$$NOW" } },
      { $merge: { into: "vehicle_ref", on: "_id", whenMatched: "keepExisting", whenNotMatched: "insert" } }
    ]);
    db2.carga_clientes_tmp.drop();
    db2.carga_vehiculos_tmp.drop();
    print("   customer_ref: " + db2.customer_ref.countDocuments() + ", vehicle_ref: " + db2.vehicle_ref.countDocuments());'

echo "3/3 Comparando tarifas y cotizaciones"
HUELLA='for (const c of ["tablas_tarifarias", "cotizaciones"]) {
  const docs = db.getSiblingDB(DB)[c].find({}, {_class: 0}).sort({_id: 1}).toArray();
  print(c + " " + docs.length + " " + require("crypto").createHash("sha256").update(EJSON.stringify(docs, {relaxed: false})).digest("hex"));
}'
EN_ORIGEN=$(docker exec "$ORIGEN" mongosh --quiet --username root --password "$ORIGEN_ROOT_PASS" \
  --authenticationDatabase admin --eval "const DB='${ORIGEN_DB}'; ${HUELLA}")
EN_DESTINO=$(docker exec "$DESTINO" mongosh --quiet --username root --password "$ROOT_PASS" \
  --authenticationDatabase admin --eval "const DB='quotation_db'; ${HUELLA}")
echo "backend:"
echo "$EN_ORIGEN" | sed 's/^/   /'
echo "quotation-service:"
echo "$EN_DESTINO" | sed 's/^/   /'

if [ "$EN_ORIGEN" != "$EN_DESTINO" ]; then
  echo "ERROR: las copias no coinciden; no hacer el corte de rutas." >&2
  exit 1
fi
echo "OK: mismas tarifas y cotizaciones; proyecciones cargadas."
