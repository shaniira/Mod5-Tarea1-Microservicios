#!/bin/sh
# Prueba de restauración de un respaldo (paso 7.7 de la ruta): restaura cada base en un MongoDB
# temporal y aislado, y compara la cantidad de documentos de cada colección con el MANIFIESTO del
# respaldo. No toca las bases de los servicios.
#
# Uso (desde la raíz del repositorio):
#   sh infra/mongo/probar-restauracion.sh respaldos/<fecha-hora>
# Sale con código 0 si todo coincide y 1 si algo falta o difiere.
set -eu

CARPETA="${1:?Uso: probar-restauracion.sh <carpeta del respaldo>}"
MANIFIESTO="$CARPETA/MANIFIESTO.txt"
[ -f "$MANIFIESTO" ] || { echo "No existe $MANIFIESTO" >&2; exit 1; }
TEMPORAL="prueba-restauracion-$$"
FALLAS=0

docker run -d --name "$TEMPORAL" mongo:8 > /dev/null
trap 'docker rm -f "$TEMPORAL" > /dev/null 2>&1' EXIT
until docker exec "$TEMPORAL" mongosh --quiet --eval 1 > /dev/null 2>&1; do sleep 1; done

grep '^archivo ' "$MANIFIESTO" | while read -r _ base sha; do
  archivo="$CARPETA/$base.archive.gz"
  if [ "$(sha256sum "$archivo" | cut -d' ' -f1)" != "$sha" ]; then
    echo "FALLA $base: el SHA-256 del archivo no coincide con el manifiesto"; exit 1
  fi
  docker exec -i "$TEMPORAL" mongorestore --quiet --archive --gzip < "$archivo"
done || FALLAS=1

while read -r _ base coleccion esperado; do
  obtenido="$(docker exec "$TEMPORAL" mongosh --quiet "$base" --eval "print(db.getCollection('$coleccion').countDocuments())" | tr -d '\r')"
  if [ "$obtenido" = "$esperado" ]; then
    printf 'OK    %-40s %s\n' "$base.$coleccion" "$obtenido"
  else
    printf 'FALLA %-40s esperado %s, restaurado %s\n' "$base.$coleccion" "$esperado" "$obtenido"
    FALLAS=1
  fi
done <<LISTA
$(grep '^coleccion ' "$MANIFIESTO")
LISTA

[ "$FALLAS" = 0 ] && echo "Restauración verificada." || echo "La restauración NO coincide con el respaldo."
exit "$FALLAS"
