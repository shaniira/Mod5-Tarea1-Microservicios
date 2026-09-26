#!/usr/bin/env bash
# Arranque de MongoDB como replica set con autenticacion (pasos 0.6 y 0.7 de la ruta).
#
# Sirve igual para un volumen nuevo y para uno con datos de antes (que no tenia usuarios):
#   1. Prepara el keyFile (obligatorio para un replica set con autenticacion).
#   2. Arranca mongod de forma temporal, sin red ni autenticacion, y crea o actualiza el usuario
#      root y los usuarios de cada servicio. Es idempotente: se puede reiniciar cuantas veces sea.
#   3. Arranca mongod definitivo con --replSet y --keyFile (la autenticacion queda obligatoria).
#
# La iniciacion del replica set (rs.initiate) la hace el healthcheck / probe, ya autenticado.
#
# Variables:
#   MONGO_ROOT_USERNAME / MONGO_ROOT_PASSWORD  usuario administrador
#   MONGO_APP_USERS     usuarios de los servicios: "usuario:base:clave,usuario2:base2:clave2"
#   MONGO_REPLICA_SET   nombre del replica set (rs0)
#   MONGO_KEYFILE_SOURCE (opcional) keyFile montado desde un Secret (Kubernetes). Si no hay, se
#                       genera uno la primera vez dentro del volumen de datos.
set -euo pipefail

DBPATH=/data/db
KEYFILE="$DBPATH/.replica-keyfile"
ROOT_USER="${MONGO_ROOT_USERNAME:-root}"
ROOT_PASS="${MONGO_ROOT_PASSWORD:?Falta MONGO_ROOT_PASSWORD}"
REPLICA_SET="${MONGO_REPLICA_SET:-rs0}"

# 1) keyFile: mongod exige que sea del usuario mongodb y sin permisos para otros.
if [ -n "${MONGO_KEYFILE_SOURCE:-}" ]; then
  cp "$MONGO_KEYFILE_SOURCE" "$KEYFILE"
elif [ ! -s "$KEYFILE" ]; then
  openssl rand -base64 756 > "$KEYFILE"
fi
chown mongodb:mongodb "$KEYFILE" "$DBPATH"
chmod 400 "$KEYFILE"

# 2) Usuarios, con mongod temporal solo en localhost y sin autenticacion.
gosu mongodb mongod --dbpath "$DBPATH" --bind_ip 127.0.0.1 --port 27018 \
  --fork --logpath /tmp/mongod-setup.log >/dev/null

mongosh --quiet --port 27018 --eval "
  function asegurar(db, usuario, clave, roles) {
    if (db.getUser(usuario)) {
      db.updateUser(usuario, { pwd: clave, roles: roles });
    } else {
      db.createUser({ user: usuario, pwd: clave, roles: roles });
    }
  }
  asegurar(db.getSiblingDB('admin'), '$ROOT_USER', '$ROOT_PASS', [{ role: 'root', db: 'admin' }]);
  const apps = '${MONGO_APP_USERS:-}'.split(',').filter(x => x.trim());
  for (const app of apps) {
    const [usuario, base, clave] = app.trim().split(':');
    asegurar(db.getSiblingDB(base), usuario, clave, [{ role: 'readWrite', db: base }]);
    print('Usuario ' + usuario + ' listo sobre ' + base);
  }
"
gosu mongodb mongod --dbpath "$DBPATH" --shutdown >/dev/null

# 3) mongod definitivo: replica set y autenticacion obligatoria (--keyFile la activa).
exec gosu mongodb mongod --replSet "$REPLICA_SET" --bind_ip_all --keyFile "$KEYFILE"
