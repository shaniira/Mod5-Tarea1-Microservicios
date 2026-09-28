#!/bin/sh
# Reconciliación de proyecciones contra sus fuentes (paso 7.3 de la ruta). Solo lee.
#
# Cada proyección es una copia de datos de otro servicio mantenida por eventos. Este script
# compara, por id y por los campos que la proyección copia, la fuente con cada copia:
#
#   customer_db.clientes      -> notification_db.customer_contacts (teléfono, correo)
#                             -> identity_db.customer_email_index    (correo)
#                             -> quotation_db.customer_ref            (fecha de nacimiento, activo)
#   customer_db.vehiculos     -> quotation_db.vehicle_ref             (cliente, año, tipo, uso)
#   policy_db.polizas         -> claims_db.policy_ref                 (estado)
#   claims_db.siniestros      -> policy_db.claim_ref                  (póliza, abierto, responsable)
#   quotation_db.cotizaciones -> policy_db.accepted_quotes            (las ACEPTADA deben estar;
#                                                                      no debe haber otras que
#                                                                      no sean ACEPTADA o EMITIDA)
#
# Uso (desde la raíz del repositorio, con el stack de Compose levantado):
#   sh infra/operacion/reconciliar.sh
# Sale con código 0 si todo coincide y 1 si hay diferencias (para programarlo cada noche con cron
# o el Programador de tareas y alertar si falla). Cómo corregir cada diferencia: guía de operación
# (doc/5. Microservicios/o_GUIA-OPERACION.md).
set -eu

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
DIFERENCIAS=0

# consulta <contenedor> <base> <expresión que devuelve un arreglo de líneas "id|campo|campo">
consulta() {
  MSYS_NO_PATHCONV=1 docker exec "$1" sh -c 'mongosh --quiet \
    -u "${MONGO_ROOT_USERNAME:-$MONGO_INITDB_ROOT_USERNAME}" \
    -p "${MONGO_ROOT_PASSWORD:-$MONGO_INITDB_ROOT_PASSWORD}" \
    --authenticationDatabase admin "$0" --eval "$1"' "$2" "
    const fecha = x => x ? new Date(x).toISOString().slice(0, 10) : '';
    const v = x => (x === undefined || x === null) ? '' : String(x);
    const lineas = ($3);
    print(lineas.join('\n'));" | tr -d '\r' | sed '/^$/d' | sort
}

ids() { cut -d'|' -f1 "$1" | sort -u; }

# comparar <nombre> <archivo fuente> <archivo proyección>
comparar() {
  nombre="$1"; fuente="$2"; copia="$3"
  ids "$fuente" > "$TMP/f.ids"; ids "$copia" > "$TMP/c.ids"
  faltan=$(comm -23 "$TMP/f.ids" "$TMP/c.ids" | wc -l | tr -d ' ')
  sobran=$(comm -13 "$TMP/f.ids" "$TMP/c.ids" | wc -l | tr -d ' ')
  # Mismo id con distintos campos: líneas distintas cuyo id está en las dos.
  comm -12 "$TMP/f.ids" "$TMP/c.ids" > "$TMP/ambos.ids"
  distintos=$(comm -3 "$fuente" "$copia" | tr -d '\t' | cut -d'|' -f1 | sort -u | comm -12 - "$TMP/ambos.ids" | wc -l | tr -d ' ')
  if [ "$faltan$sobran$distintos" = "000" ]; then
    printf 'OK    %-48s %s = %s\n' "$nombre" "$(wc -l < "$fuente" | tr -d ' ')" "$(wc -l < "$copia" | tr -d ' ')"
  else
    DIFERENCIAS=1
    printf 'DIFF  %-48s fuente %s, copia %s: faltan %s, sobran %s, distintos %s\n' "$nombre" \
      "$(wc -l < "$fuente" | tr -d ' ')" "$(wc -l < "$copia" | tr -d ' ')" "$faltan" "$sobran" "$distintos"
    comm -23 "$TMP/f.ids" "$TMP/c.ids" | head -5 | sed 's/^/        falta:    /'
    comm -13 "$TMP/f.ids" "$TMP/c.ids" | head -5 | sed 's/^/        sobra:    /'
    comm -3 "$fuente" "$copia" | tr -d '\t' | cut -d'|' -f1 | sort -u | comm -12 - "$TMP/ambos.ids" | head -5 | sed 's/^/        distinto: /'
  fi
}

echo "Reconciliación de proyecciones ($(date -u +%Y-%m-%dT%H:%M:%SZ))"

consulta customer-mongodb customer_db 'db.clientes.find().toArray().map(d => [d._id, v(d.telefono), v(d.correo)].join("|"))' > "$TMP/clientes-contacto"
consulta andina-notification-mongodb notification_db 'db.customer_contacts.find().toArray().map(d => [d._id, v(d.telefono), v(d.correo)].join("|"))' > "$TMP/contactos"
comparar "clientes -> customer_contacts (notification)" "$TMP/clientes-contacto" "$TMP/contactos"

consulta customer-mongodb customer_db 'db.clientes.find().toArray().map(d => [d._id, v(d.correo)].join("|"))' > "$TMP/clientes-correo"
consulta andina-identity-mongodb identity_db 'db.customer_email_index.find().toArray().map(d => [d._id, v(d.correo)].join("|"))' > "$TMP/correos"
comparar "clientes -> customer_email_index (identity)" "$TMP/clientes-correo" "$TMP/correos"

consulta customer-mongodb customer_db 'db.clientes.find().toArray().map(d => [d._id, fecha(d.fechaNacimiento), v(d.activo)].join("|"))' > "$TMP/clientes-ref"
consulta quotation-mongodb quotation_db 'db.customer_ref.find().toArray().map(d => [d._id, fecha(d.fechaNacimiento), v(d.activo)].join("|"))' > "$TMP/customer-ref"
comparar "clientes -> customer_ref (quotation)" "$TMP/clientes-ref" "$TMP/customer-ref"

consulta customer-mongodb customer_db 'db.vehiculos.find().toArray().map(d => [d._id, d.clienteId, v(d.anioFabricacion), v(d.tipo), v(d.uso)].join("|"))' > "$TMP/vehiculos"
consulta quotation-mongodb quotation_db 'db.vehicle_ref.find().toArray().map(d => [d._id, d.clienteId, v(d.anioFabricacion), v(d.tipo), v(d.uso)].join("|"))' > "$TMP/vehicle-ref"
comparar "vehiculos -> vehicle_ref (quotation)" "$TMP/vehiculos" "$TMP/vehicle-ref"

consulta policy-mongodb policy_db 'db.polizas.find().toArray().map(d => [d._id, v(d.estado)].join("|"))' > "$TMP/polizas"
consulta claims-mongodb claims_db 'db.policy_ref.find().toArray().map(d => [d._id, v(d.estado)].join("|"))' > "$TMP/policy-ref"
comparar "polizas -> policy_ref (claims)" "$TMP/polizas" "$TMP/policy-ref"

consulta claims-mongodb claims_db 'db.siniestros.find().toArray().map(d => [d._id, d.polizaId, v(!["LIQUIDADO", "RECHAZADO"].includes(d.estado)), v(d.responsabilidadAsegurado)].join("|"))' > "$TMP/siniestros"
consulta policy-mongodb policy_db 'db.claim_ref.find().toArray().map(d => [d._id, d.polizaId, v(d.abierto), v(d.responsabilidadAsegurado)].join("|"))' > "$TMP/claim-ref"
comparar "siniestros -> claim_ref (policy)" "$TMP/siniestros" "$TMP/claim-ref"

# accepted_quotes: toda cotización ACEPTADA debe estar; y lo que está debe ser ACEPTADA o EMITIDA
# (una EMITIDA de antes del corte puede no estar: la carga inicial solo copió las ACEPTADA).
consulta quotation-mongodb quotation_db 'db.cotizaciones.find({ estado: "ACEPTADA" }).toArray().map(d => d._id)' > "$TMP/aceptadas"
consulta quotation-mongodb quotation_db 'db.cotizaciones.find({ estado: { $in: ["ACEPTADA", "EMITIDA"] } }).toArray().map(d => d._id)' > "$TMP/aceptadas-o-emitidas"
consulta policy-mongodb policy_db 'db.accepted_quotes.find().toArray().map(d => d._id)' > "$TMP/accepted-quotes"
comm -12 "$TMP/accepted-quotes" "$TMP/aceptadas-o-emitidas" > "$TMP/accepted-validas"
cat "$TMP/aceptadas" "$TMP/accepted-validas" | sort -u > "$TMP/esperadas"
comm -12 "$TMP/accepted-quotes" "$TMP/esperadas" | cat - "$TMP/aceptadas" | sort -u > "$TMP/esperadas2"
comparar "cotizaciones ACEPTADA -> accepted_quotes (policy)" "$TMP/esperadas2" "$TMP/accepted-quotes"

if [ "$DIFERENCIAS" = 0 ]; then
  echo "Todo coincide."
else
  echo "Hay diferencias: ver la guía de operación (sección Reconciliación)."
fi
exit "$DIFERENCIAS"
