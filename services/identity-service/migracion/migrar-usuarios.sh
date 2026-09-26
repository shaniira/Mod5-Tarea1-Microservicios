#!/usr/bin/env sh
# Paso 2.7 de la ruta: copia la coleccion "usuarios" del MongoDB del backend a identity_db y
# compara las dos copias. Los hashes BCrypt y los secretos MFA se copian tal cual (nadie tiene que
# cambiar su contrasena ni volver a configurar MFA).
#
# Uso (desde cualquier carpeta, con el stack de Arquitectura-Clean levantado):
#   sh services/identity-service/migracion/migrar-usuarios.sh
#
# Se puede repetir: cada ejecucion reemplaza la copia (--drop). La coleccion del backend no se
# toca: es el plan de reversa mientras dure la fase 2.
set -eu

ORIGEN="${ORIGEN_CONTENEDOR:-andina-clean-mongodb}"
ORIGEN_DB="${ORIGEN_DB:-andina_seguros_clean}"
DESTINO="${DESTINO_CONTENEDOR:-andina-identity-mongodb}"
ROOT_PASS="${IDENTITY_MONGO_ROOT_PASSWORD:-identity-root-local}"

echo "1/3 Copiando ${ORIGEN_DB}.usuarios -> identity_db.usuarios"
docker exec "$ORIGEN" mongodump --quiet --db "$ORIGEN_DB" --collection usuarios --archive \
  | docker exec -i "$DESTINO" mongorestore --quiet --archive --drop \
      --username root --password "$ROOT_PASS" --authenticationDatabase admin \
      --nsFrom "${ORIGEN_DB}.usuarios" --nsTo "identity_db.usuarios"

# Huella de la coleccion: documentos ordenados por _id, serializados en EJSON canonico.
HUELLA='const docs = db.getSiblingDB(DB).usuarios.find({}, {_class: 0}).sort({_id: 1}).toArray();
print(docs.length + " " + require("crypto").createHash("sha256").update(EJSON.stringify(docs, {relaxed: false})).digest("hex"))'

echo "2/3 Comparando"
EN_ORIGEN=$(docker exec "$ORIGEN" mongosh --quiet --eval "const DB='${ORIGEN_DB}'; ${HUELLA}")
EN_DESTINO=$(docker exec "$DESTINO" mongosh --quiet --username root --password "$ROOT_PASS" \
  --authenticationDatabase admin --eval "const DB='identity_db'; ${HUELLA}")
echo "   backend : ${EN_ORIGEN}"
echo "   identity: ${EN_DESTINO}"

if [ "$EN_ORIGEN" != "$EN_DESTINO" ]; then
  echo "ERROR: las copias no coinciden; no hacer el corte de rutas." >&2
  exit 1
fi
echo "3/3 OK: mismo numero de usuarios y mismo contenido. Se puede hacer el corte."
