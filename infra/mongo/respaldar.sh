#!/bin/sh
# Respaldo de las bases de todos los servicios (paso 7.7 de la ruta).
#
# Uso (desde la raíz del repositorio, con el stack de Compose levantado):
#   sh infra/mongo/respaldar.sh [carpeta]          # por defecto respaldos/<fecha-hora>
#
# Deja por base un archivo <base>.archive.gz (mongodump comprimido) y un MANIFIESTO.txt con el
# SHA-256 de cada archivo y la cantidad de documentos por colección, que usa
# probar-restauracion.sh para comprobar el respaldo. respaldos/ no se versiona (datos personales).
set -eu

DESTINO="${1:-respaldos/$(date -u +%Y%m%dT%H%M%SZ)}"
mkdir -p "$DESTINO"
MANIFIESTO="$DESTINO/MANIFIESTO.txt"
echo "# Respaldo $(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$MANIFIESTO"

# contenedor:base de cada servicio (database per service)
BASES="identity-mongodb:identity_db notification-mongodb:notification_db
customer-mongodb:customer_db claims-mongodb:claims_db quotation-mongodb:quotation_db
policy-mongodb:policy_db"

mongo() {
  MSYS_NO_PATHCONV=1 docker exec "$1" sh -c "$2 -u \"\${MONGO_ROOT_USERNAME:-\$MONGO_INITDB_ROOT_USERNAME}\" \
    -p \"\${MONGO_ROOT_PASSWORD:-\$MONGO_INITDB_ROOT_PASSWORD}\" --authenticationDatabase admin $3"
}

for par in $BASES; do
  contenedor="${par%%:*}"; base="${par#*:}"
  archivo="$DESTINO/$base.archive.gz"
  mongo "$contenedor" "mongodump --quiet --archive --gzip" "--db $base" > "$archivo"
  echo "archivo $base $(sha256sum "$archivo" | cut -d' ' -f1)" >> "$MANIFIESTO"
  mongo "$contenedor" "mongosh --quiet" "$base --eval 'db.getCollectionNames().sort().forEach(c => print(\"coleccion $base \" + c + \" \" + db[c].countDocuments()))'" \
    | tr -d '\r' >> "$MANIFIESTO"
  echo "Respaldada $base ($(du -h "$archivo" | cut -f1))"
done
echo "Listo: $DESTINO"
