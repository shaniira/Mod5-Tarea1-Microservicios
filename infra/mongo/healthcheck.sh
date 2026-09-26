#!/usr/bin/env bash
# Healthcheck / probe de MongoDB: inicia el replica set la primera vez (autenticado como root) y
# solo da "sano" cuando este nodo ya es PRIMARY y acepta escrituras.
#   MONGO_REPLICA_HOST  host:puerto con el que los clientes llegan a este nodo (mongodb:27017)
set -euo pipefail

mongosh --quiet \
  --username "${MONGO_ROOT_USERNAME:-root}" --password "${MONGO_ROOT_PASSWORD:?}" \
  --authenticationDatabase admin \
  --eval "
    try { rs.status(); } catch (e) {
      if (e.codeName !== 'NotYetInitialized') { throw e; }
      rs.initiate({ _id: '${MONGO_REPLICA_SET:-rs0}', members: [{ _id: 0, host: '${MONGO_REPLICA_HOST:?}' }] });
    }
    quit(db.hello().isWritablePrimary ? 0 : 1);
  "
