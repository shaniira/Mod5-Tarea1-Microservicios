#!/bin/bash
# Pruebas de caos (paso 7.1 de la ruta): apaga un componente a la vez, comprueba por el gateway la
# degradación planificada (propuesta, secciones 6.2 y 6.4), lo vuelve a levantar y comprueba que el
# sistema se recupera sin perder eventos.
#
# Uso (desde la raíz del repositorio, con el stack levantado y WhatsApp y JSON.pe simulados):
#   bash infra/operacion/caos.sh                 # los casos básicos (~25 min)
#   bash infra/operacion/caos.sh todos           # además los largos (~40 min)
#   bash infra/operacion/caos.sh claims rabbitmq # solo algunos
# Casos básicos: claims notification customer quotation policy identity rabbitmq redis
#                mongo-policy whatsapp jsonpe
# Casos de consistencia: renovacion-reciente (siniestro justo antes de renovar), renovacion-claims
#                (claims caído al renovar), revocacion-redis (token revocado con Redis caído)
# Casos largos:  replicas (2 réplicas de policy), relay-lote (lote acumulado con 2 réplicas),
#                identity-larga (identity caído 6 minutos)
# Sale con código 1 si algún resultado no es el esperado.
set -u

BASE="${BASE:-http://localhost:8080}"
FALLAS=0
CID="caos-$(date +%s)"

ok()    { printf '  OK     %s\n' "$*"; }
falla() { printf '  FALLA  %s\n' "$*"; FALLAS=1; }
espera() { # espera <descripción> <segundos> <comando...>: reintenta hasta que el comando salga 0
  local desc="$1" max="$2"; shift 2
  for ((i = 0; i < max; i += 3)); do "$@" > /dev/null 2>&1 && { ok "$desc (${i} s)"; return 0; }; sleep 3; done
  falla "$desc (no ocurrió en ${max} s)"; return 1
}

token() {
  for _ in 1 2 3 4 5 6; do
    T=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
      -d '{"username":"admin2","password":"admin2"}' | node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{try{const t=JSON.parse(s).token;console.log(typeof t==='string'?t:t.token)}catch(e){}})")
    [ -n "$T" ] && return 0; sleep 1.5
  done
  echo "No se pudo iniciar sesión" >&2; exit 2
}

# api <método> <ruta> [json] -> imprime "código cuerpo"
api() {
  local args=(-s -m 20 -w '\n%{http_code}' -X "$1" -H "Authorization: Bearer $T" -H "X-Correlation-Id: $CID" \
    -H 'Content-Type: application/json' "$BASE$2")
  [ -n "${3:-}" ] && args+=(-d "$3")
  curl "${args[@]}" | node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{const l=s.trim().split('\n');const c=l.pop();console.log(c+' '+l.join(''))})"
  sleep 0.1
}
codigo() { api "$@" | cut -d' ' -f1; }
campo() { node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{try{console.log(JSON.parse(s.slice(s.indexOf(' ')+1))['$1'])}catch(e){}})"; }
espera_codigo() { # espera_codigo <esperado> <método> <ruta> [json]
  local esperado="$1"; shift; [ "$(codigo "$@")" = "$esperado" ]
}

mongo() { # mongo <contenedor> <base> <expresión>
  MSYS_NO_PATHCONV=1 docker exec "$1" sh -c 'mongosh --quiet -u "${MONGO_ROOT_USERNAME:-$MONGO_INITDB_ROOT_USERNAME}" -p "${MONGO_ROOT_PASSWORD:-$MONGO_INITDB_ROOT_PASSWORD}" --authenticationDatabase admin "$0" --eval "$1"' "$2" "$3" | tr -d '\r'
}
mensajes() { docker exec andina-clean-rabbitmq-1 rabbitmqctl -q list_queues name messages 2>/dev/null | awk -v q="$1" '$1==q{print $2}'; }
sano() { docker inspect -f '{{.State.Health.Status}}' "$1" 2>/dev/null | grep -qx healthy; }
apagar() { docker stop "$1" > /dev/null && echo "  -- $1 detenido"; }
encender() { docker start "$1" > /dev/null && echo "  -- $1 iniciado"; espera "$1 vuelve a estar sano" 240 sano "$1"; }

cotizar_y_aceptar() { # deja en QID una cotización aceptada
  QID=$(api POST /api/cotizaciones "{\"clienteId\":\"$CLIENTE\",\"vehiculoId\":\"$VEHICULO\",\"siniestrosResponsables\":0}" | campo id)
  api PATCH "/api/cotizaciones/$QID/aceptar" > /dev/null
}
emitir() { api POST /api/polizas "{\"cotizacionId\":\"$1\",\"inicioVigencia\":\"2026-11-01\"}"; }
en_accepted_quotes() { [ "$(mongo policy-mongodb policy_db "print(db.accepted_quotes.countDocuments({_id:'$1'}))")" = 1 ]; }
en_policy_ref() { [ "$(mongo claims-mongodb claims_db "print(db.policy_ref.countDocuments({_id:'$1'}))")" = 1 ]; }
cotizacion_emitida() { [ "$(codigo GET "/api/cotizaciones/$1")" = 200 ] && api GET "/api/cotizaciones/$1" | grep -q '"estado":"EMITIDA"'; }
outbox_sin_pendientes() { [ "$(mongo policy-mongodb policy_db "print(db.outbox.countDocuments({status:'PENDING'}))")" = 0 ]; }
whatsapp_enviado() { docker logs andina-notification-service 2>&1 | grep "$1" | grep -q "ENVIADA"; }

preparar() {
  token
  CLIENTE=$(api GET /api/clientes | node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{const j=JSON.parse(s.slice(s.indexOf(' ')+1));console.log(j[0].id)})")
  VEHICULO=$(api GET "/api/clientes/$CLIENTE/vehiculos" | node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{const j=JSON.parse(s.slice(s.indexOf(' ')+1));console.log(j.length?j[0].id:'')})")
  [ -n "$VEHICULO" ] || { echo "El cliente $CLIENTE no tiene vehículos" >&2; exit 2; }
}

caso_claims() {
  echo "== claims-service caído: se emite y se renueva; los siniestros responden 503"
  cotizar_y_aceptar; sleep 4
  apagar claims-service
  espera "siniestros responden 503 (fallback)" 30 espera_codigo 503 GET "/api/polizas/$(uuidgen 2>/dev/null || echo 00000000-0000-4000-8000-000000000000)/siniestros"
  R=$(emitir "$QID"); PID=$(echo "$R" | campo id)
  [ "${R%% *}" = 201 ] && ok "emitir sin claims: 201" || falla "emitir sin claims: ${R:0:120}"
  [ "$(mensajes claims.policy.events)" -ge 1 ] && ok "policy.issued espera en claims.policy.events" || falla "policy.issued no quedó en la cola de claims"
  encender claims-service
  espera "claims aplica la póliza emitida mientras estaba caído (policy_ref)" 90 en_policy_ref "$PID"
}

caso_notification() {
  echo "== notification-service caído: todo funciona salvo el WhatsApp, que sale al volver"
  cotizar_y_aceptar; sleep 4
  apagar andina-notification-service
  R=$(emitir "$QID"); NUM=$(echo "$R" | campo numero)
  [ "${R%% *}" = 201 ] && ok "emitir sin notification: 201" || falla "emitir sin notification: ${R:0:120}"
  sleep 3
  [ "$(mensajes notification.policy.events)" -ge 1 ] && ok "policy.issued espera en notification.policy.events" || falla "el evento no quedó en la cola de notification"
  encender andina-notification-service
  espera "el WhatsApp de $NUM sale al volver" 90 whatsapp_enviado "$NUM"
}

caso_customer() {
  echo "== customer-service caído: se cotiza con la proyección; clientes responde 503"
  apagar customer-service
  espera "clientes responde 503 (fallback)" 30 espera_codigo 503 GET /api/clientes
  C=$(codigo POST /api/cotizaciones "{\"clienteId\":\"$CLIENTE\",\"vehiculoId\":\"$VEHICULO\",\"siniestrosResponsables\":0}")
  [ "$C" = 201 ] && ok "cotizar cliente conocido sin customer: 201 (customer_ref/vehicle_ref)" || falla "cotizar sin customer: $C"
  encender customer-service
}

caso_quotation() {
  echo "== quotation-service caído: cotizar responde 503; se emite una cotización ya aceptada"
  cotizar_y_aceptar; sleep 4
  apagar quotation-service
  espera "cotizaciones responde 503 (fallback)" 30 espera_codigo 503 GET /api/cotizaciones
  R=$(emitir "$QID")
  [ "${R%% *}" = 201 ] && ok "emitir sin quotation: 201 (accepted_quotes)" || falla "emitir sin quotation: ${R:0:120}"
  encender quotation-service
  espera "quotation marca la cotización EMITIDA al volver" 90 cotizacion_emitida "$QID"
}

caso_policy() {
  echo "== policy-service caído: pólizas 503; aceptar una cotización sigue funcionando"
  apagar policy-service
  espera "pólizas responde 503 (fallback)" 30 espera_codigo 503 GET /api/polizas
  cotizar_y_aceptar
  [ -n "$QID" ] && ok "cotizar y aceptar sin policy" || falla "no se pudo cotizar sin policy"
  encender policy-service
  espera "quote.accepted llega a accepted_quotes al volver" 90 en_accepted_quotes "$QID"
}

caso_identity() {
  echo "== identity-service caído: login 503; los tokens ya emitidos siguen sirviendo"
  local ID; ID=$(docker ps -q --filter name=identity-service)
  docker stop $ID > /dev/null && echo "  -- identity-service detenido"
  espera "login responde 503 (fallback)" 30 espera_codigo 503 POST /api/auth/login '{"username":"admin2","password":"admin2"}'
  C=$(codigo GET /api/polizas); [ "$C" = 200 ] && ok "token vigente sin identity: 200" || falla "token vigente sin identity: $C"
  docker start $ID > /dev/null && echo "  -- identity-service iniciado"
  espera "login vuelve a funcionar" 120 espera_codigo 200 POST /api/auth/login '{"username":"admin2","password":"admin2"}'
  token
}

caso_identity_larga() {
  echo "== identity-service caído 6 minutos (más que la caché de claves JWKS de 5 min)"
  local ID; ID=$(docker ps -q --filter name=identity-service)
  docker stop $ID > /dev/null && echo "  -- identity-service detenido"
  sleep 370
  for r in /api/polizas /api/clientes /api/cotizaciones /api/tablas-tarifarias; do
    C=$(codigo GET "$r"); [ "$C" = 200 ] && ok "token vigente tras 6 min sin identity, $r: 200" || falla "token vigente tras 6 min sin identity, $r: $C"
  done
  docker start $ID > /dev/null && echo "  -- identity-service iniciado"
  espera "login vuelve a funcionar" 120 espera_codigo 200 POST /api/auth/login '{"username":"admin2","password":"admin2"}'
  token
}

caso_rabbitmq() {
  echo "== RabbitMQ caído durante una emisión: no se pierde ningún evento (Outbox)"
  cotizar_y_aceptar; sleep 4
  apagar andina-clean-rabbitmq-1
  R=$(emitir "$QID"); PID=$(echo "$R" | campo id); NUM=$(echo "$R" | campo numero)
  [ "${R%% *}" = 201 ] && ok "emitir sin RabbitMQ: 201" || falla "emitir sin RabbitMQ: ${R:0:120}"
  P=$(mongo policy-mongodb policy_db "print(db.outbox.countDocuments({aggregateId:'$PID',status:'PENDING'}))")
  [ "$P" -ge 1 ] && ok "policy.issued queda PENDING en el Outbox ($P)" || falla "el evento no quedó en el Outbox"
  encender andina-clean-rabbitmq-1
  espera "el Outbox publica al volver (0 pendientes)" 120 outbox_sin_pendientes
  espera "claims recibe la póliza" 120 en_policy_ref "$PID"
  espera "quotation marca la cotización EMITIDA" 120 cotizacion_emitida "$QID"
  espera "notification envía el WhatsApp" 120 whatsapp_enviado "$NUM"
}

caso_redis() {
  echo "== Redis caído: el gateway sigue atendiendo (rate limiter) y el login sigue funcionando"
  apagar andina-clean-redis
  sleep 3
  C=$(codigo GET /api/polizas); [ "$C" = 200 ] && ok "lecturas sin Redis: 200" || falla "lecturas sin Redis: $C"
  C=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin2","password":"admin2"}')
  echo "  INFO   login sin Redis: $C (el estado de MFA/OAuth y la revocación viven en Redis)"
  encender andina-clean-redis
}

caso_mongo_policy() {
  echo "== MongoDB de policy caído: solo pólizas deja de atender; el resto sigue"
  apagar policy-mongodb
  espera "pólizas deja de responder 200" 60 bash -c "[ \"\$(curl -s -o /dev/null -w '%{http_code}' -m 10 -H 'Authorization: Bearer $T' $BASE/api/polizas)\" != 200 ]"
  echo "  INFO   pólizas responde: $(codigo GET /api/polizas)"
  C=$(codigo GET /api/clientes); [ "$C" = 200 ] && ok "clientes sigue en 200" || falla "clientes: $C"
  C=$(codigo GET /api/cotizaciones); [ "$C" = 200 ] && ok "cotizaciones sigue en 200" || falla "cotizaciones: $C"
  espera "policy-service deja de estar listo (readiness)" 60 bash -c "! docker exec andina-api-gateway wget -qO- http://policy-service:8080/actuator/health/readiness"
  encender policy-mongodb
  espera "policy-service vuelve a estar listo" 120 docker exec andina-api-gateway wget -qO- http://policy-service:8080/actuator/health/readiness
  espera "pólizas vuelve a 200" 60 espera_codigo 200 GET /api/polizas
}

caso_whatsapp() {
  echo "== WhatsApp caído: el circuito se abre, el consumo se pausa y nada va a la DLQ"
  docker stop andina-whatsapp-mock > /dev/null && echo "  -- whatsapp-mock detenido"
  local NUMS=()
  for _ in 1 2 3 4 5 6; do cotizar_y_aceptar; sleep 3; NUMS+=("$(emitir "$QID" | campo numero)"); done
  espera "el listener de pólizas se pausa (circuito abierto)" 120 bash -c "docker logs andina-notification-service 2>&1 | tail -200 | grep -qi 'paus'"
  D=$(mensajes notification.policy.events.dlq); [ "${D:-0}" = 0 ] && ok "DLQ de pólizas en 0" || falla "hay $D mensaje(s) en la DLQ"
  docker start andina-whatsapp-mock > /dev/null && echo "  -- whatsapp-mock iniciado"
  espera "los WhatsApp retenidos salen al recuperarse (${NUMS[-1]})" 240 whatsapp_enviado "${NUMS[-1]}"
  D=$(mensajes notification.policy.events.dlq); [ "${D:-0}" = 0 ] && ok "DLQ sigue en 0" || falla "hay $D mensaje(s) en la DLQ"
}

caso_jsonpe() {
  echo "== JSON.pe caído: una placa ya consultada sale de la caché; una nueva responde 503"
  local PLACA=ABC123
  api GET "/api/vehiculos/informacion-externa?placa=$PLACA" > /dev/null
  docker stop jsonpe-mock > /dev/null && echo "  -- jsonpe-mock detenido"
  C=$(codigo GET "/api/vehiculos/informacion-externa?placa=$PLACA"); [ "$C" = 200 ] && ok "placa en caché: 200" || falla "placa en caché: $C"
  C=$(codigo GET "/api/vehiculos/informacion-externa?placa=ZZZ$(date +%S)9"); echo "  INFO   placa nueva sin JSON.pe: $C"
  case "$C" in 500|000) falla "placa nueva sin JSON.pe: $C (no controlado)";; *) ok "placa nueva sin JSON.pe: $C (error controlado, se puede ingresar a mano)";; esac
  docker start jsonpe-mock > /dev/null && echo "  -- jsonpe-mock iniciado"
}

emitir_en() { # emitir_en <contenedor> <cotización>: emite llamando a esa réplica directamente (sin gateway)
  docker exec "$1" wget -qO- --header "Authorization: Bearer $T" --header 'Content-Type: application/json' \
    --header "X-Correlation-Id: $CID" --post-data "{\"cotizacionId\":\"$2\",\"inicioVigencia\":\"2026-11-01\"}" \
    http://localhost:8080/api/polizas 2>/dev/null \
    | node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{try{console.log(JSON.parse(s).id)}catch(e){}})"
}

caso_replicas() {
  # En Compose el gateway no reparte entre réplicas (reutiliza la conexión abierta; en Kubernetes
  # reparte el Service), así que se emite llamando a cada réplica directamente.
  echo "== 2 réplicas de policy-service: emiten las dos, cada evento sale una vez y el relay cambia de dueño"
  if ! docker inspect policy-service-replica2 > /dev/null 2>&1; then
    (docker compose -f docker-compose.yml -f infra/observability/docker-compose.observability.yml \
      run -d --no-deps --use-aliases --name policy-service-replica2 policy-service > /dev/null)
  fi
  espera "segunda réplica sana" 240 sano policy-service-replica2
  local IDS=() R i id n VACIOS=0 DUP=0 FALTA=0
  for i in $(seq 1 10); do
    cotizar_y_aceptar; sleep 4
    if [ $((i % 2)) = 0 ]; then R=policy-service; else R=policy-service-replica2; fi
    IDS+=("$(emitir_en "$R" "$QID")")
  done
  for id in "${IDS[@]}"; do [ -n "$id" ] || VACIOS=$((VACIOS + 1)); done
  [ "$VACIOS" = 0 ] && ok "las dos réplicas emitieron (5 y 5 de 10)" || falla "$VACIOS emisión(es) fallaron"
  sleep 10
  for id in "${IDS[@]}"; do
    [ -n "$id" ] || continue
    n=$(docker logs claims-service 2>&1 | grep -c "PolicyIssued póliza $id")
    [ "$n" = 1 ] || { [ "$n" = 0 ] && FALTA=$((FALTA + 1)) || DUP=$((DUP + 1)); }
  done
  [ "$DUP$FALTA" = 00 ] && ok "cada policy.issued llegó exactamente una vez a claims" || falla "duplicados $DUP, faltantes $FALTA"
  local DUENO ACTIVO=policy-service OTRO=policy-service-replica2
  DUENO=$(mongo policy-mongodb policy_db "print(db.outbox_lock.findOne().owner)")
  [ "$(docker exec policy-service-replica2 hostname)" = "${DUENO%-*}" ] && { ACTIVO=policy-service-replica2; OTRO=policy-service; }
  echo "  INFO   turno del relay: $DUENO ($ACTIVO)"
  apagar "$ACTIVO"
  cotizar_y_aceptar; sleep 4
  local PID; PID=$(emitir_en "$OTRO" "$QID")
  [ -n "$PID" ] && ok "emitir en $OTRO con la dueña del turno caída" || falla "no se pudo emitir en $OTRO"
  espera "$OTRO toma el turno y publica (lease de 15 s)" 90 en_policy_ref "$PID"
  docker logs "$OTRO" 2>&1 | grep -o 'la réplica [^ ]* toma el turno' | tail -1 | sed 's/^/  INFO   /'
  docker start "$ACTIVO" > /dev/null; espera "$ACTIVO vuelve" 240 sano "$ACTIVO"
  docker rm -f policy-service-replica2 > /dev/null && echo "  -- segunda réplica eliminada"
}

caso_relay_lote() {
  # El caso difícil del turno: un lote grande pendiente y dos réplicas compitiendo por publicarlo.
  echo "== Lote acumulado con 2 réplicas: al volver RabbitMQ cada evento sale exactamente una vez"
  if ! docker inspect policy-service-replica2 > /dev/null 2>&1; then
    (docker compose -f docker-compose.yml -f infra/observability/docker-compose.observability.yml \
      run -d --no-deps --use-aliases --name policy-service-replica2 policy-service > /dev/null)
  fi
  espera "segunda réplica sana" 240 sano policy-service-replica2
  local QIDS=() IDS=() i R id n DUP=0 FALTA=0
  for i in $(seq 1 20); do cotizar_y_aceptar; QIDS+=("$QID"); done; sleep 5
  apagar andina-clean-rabbitmq-1
  for i in $(seq 1 20); do
    if [ $((i % 2)) = 0 ]; then R=policy-service; else R=policy-service-replica2; fi
    IDS+=("$(emitir_en "$R" "${QIDS[$((i - 1))]}")")
  done
  local P; P=$(mongo policy-mongodb policy_db "print(db.outbox.countDocuments({status:'PENDING'}))")
  [ "$P" -ge 20 ] && ok "20 emisiones con RabbitMQ caído: $P eventos acumulados en el Outbox" || falla "solo $P eventos pendientes"
  encender andina-clean-rabbitmq-1
  espera "el lote se publica (0 pendientes)" 180 outbox_sin_pendientes
  sleep 10
  for id in "${IDS[@]}"; do
    n=$(docker logs claims-service 2>&1 | grep -c "PolicyIssued póliza $id")
    [ "$n" = 1 ] || { [ "$n" = 0 ] && FALTA=$((FALTA + 1)) || DUP=$((DUP + 1)); }
  done
  [ "$DUP$FALTA" = 00 ] && ok "los 20 policy.issued llegaron exactamente una vez a claims" || falla "duplicados $DUP, faltantes $FALTA"
  echo "  INFO   dueña del turno: $(mongo policy-mongodb policy_db "print(db.outbox_lock.findOne().owner)")"
  docker rm -f policy-service-replica2 > /dev/null && echo "  -- segunda réplica eliminada"
}

CASOS=("$@")
# --- Fase 7: consistencia en la renovación (CP en el paso irreversible) ---------------------------
codigo_de() { node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>{const i=s.indexOf(' ');let c='';try{c=JSON.parse(s.slice(i+1)).codigo||''}catch(e){};console.log(s.slice(0,i)+' '+c)})"; }
renovacion_aprobada() { # deja en PID una póliza emitida y en RID su renovación aprobada
  cotizar_y_aceptar; sleep 4
  PID=$(emitir "$QID" | campo id); sleep 4
  RID=$(api POST "/api/renovaciones/poliza/$PID/evaluar" | campo id)
  api PATCH "/api/renovaciones/$RID/aprobar" > /dev/null
}

caso_renovacion_reciente() {
  echo "== Siniestro registrado justo antes de generar la renovación: claims lo confirma y no se renueva"
  renovacion_aprobada
  local SID R
  SID=$(api POST "/api/polizas/$PID/siniestros" '{"fecha":"2026-11-05","tipo":"CHOQUE","montoEstimado":700,"responsabilidadAsegurado":false,"gravedad":"LEVE","estado":"REPORTADO"}' | campo id)
  # Inmediatamente, sin esperar a que el evento llegue a la copia claim_ref de policy.
  R=$(api POST "/api/renovaciones/$RID/generar-poliza" | codigo_de)
  [ "$R" = "422 SINIESTROS_PENDIENTES" ] && ok "generar con el siniestro recién registrado: 422 SINIESTROS_PENDIENTES" || falla "generar con siniestro reciente: $R"
  api PATCH "/api/polizas/$PID/siniestros/$SID/estado" '{"estado":"LIQUIDADO"}' > /dev/null
  R=$(api POST "/api/renovaciones/$RID/generar-poliza" | codigo_de)
  [ "$R" = "422 RENOVACION_DESACTUALIZADA" ] && ok "con el siniestro ya cerrado, la propuesta vieja pide reevaluar: 422 RENOVACION_DESACTUALIZADA" || falla "propuesta desactualizada: $R"
  sleep 4
  RID=$(api POST "/api/renovaciones/poliza/$PID/evaluar" | campo id)
  api PATCH "/api/renovaciones/$RID/aprobar" > /dev/null
  R=$(api POST "/api/renovaciones/$RID/generar-poliza" | cut -d' ' -f1)
  [ "$R" = 201 ] && ok "reevaluada con el siniestro, se genera la renovada: 201" || falla "generar tras reevaluar: $R"
}

caso_renovacion_claims() {
  echo "== claims-service caído al generar una renovación: 503 y no se renueva (se prefiere error a dato viejo)"
  renovacion_aprobada
  apagar claims-service
  local R; R=$(api POST "/api/renovaciones/$RID/generar-poliza" | codigo_de)
  [ "$R" = "503 SINIESTROS_NO_DISPONIBLE" ] && ok "generar sin claims: 503 SINIESTROS_NO_DISPONIBLE" || falla "generar sin claims: $R"
  api GET "/api/polizas/$PID" | grep -q '"estado":"VIGENTE"' && ok "la póliza original sigue VIGENTE" || falla "la póliza original cambió de estado"
  encender claims-service
  R=$(api POST "/api/renovaciones/$RID/generar-poliza" | cut -d' ' -f1)
  [ "$R" = 201 ] && ok "con claims de vuelta se genera la renovada: 201" || falla "generar al volver claims: $R"
}

# --- Fase 7: revocación con Redis caído (copia local en el gateway) ------------------------------
caso_revocacion_redis() {
  echo "== Redis caído: un token con sesión cerrada sigue rechazado (copia local); uno vigente sigue sirviendo"
  local T_CERRADO C
  T_CERRADO=$(T= ; token; echo "$T"); token
  C=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/auth/logout" -H "Authorization: Bearer $T_CERRADO")
  echo "  INFO   logout: $C"
  C=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $T_CERRADO" "$BASE/api/polizas")
  [ "$C" = 401 ] && ok "token con sesión cerrada, Redis sano: 401" || falla "token con sesión cerrada: $C"
  sleep 7 # que la copia local del gateway se refresque (cada 5 s)
  apagar andina-clean-redis
  C=$(curl -s -o /dev/null -w '%{http_code}' -m 20 -H "Authorization: Bearer $T_CERRADO" "$BASE/api/polizas")
  [ "$C" = 401 ] && ok "token con sesión cerrada, Redis caído: 401 (copia local)" || falla "token revocado con Redis caído: $C"
  C=$(codigo GET /api/polizas); [ "$C" = 200 ] && ok "token vigente, Redis caído: 200" || falla "token vigente con Redis caído: $C"
  echo "  INFO   decisiones con la copia: $(docker exec andina-api-gateway wget -qO- http://localhost:8080/actuator/prometheus 2>/dev/null | grep '^gateway_revocaciones_copia_usos_total' | awk '{print $2}')"
  encender andina-clean-redis
}

BASICOS=(claims notification customer quotation policy identity rabbitmq redis mongo-policy whatsapp jsonpe)
# Sin argumentos: los casos básicos (~25 min). "todos" suma los largos: réplicas, lote con 2
# réplicas e identity caído 6 minutos (~40 min en total).
[ ${#CASOS[@]} -eq 0 ] && CASOS=("${BASICOS[@]}")
[ "${CASOS[*]}" = todos ] && CASOS=("${BASICOS[@]}" renovacion-reciente renovacion-claims revocacion-redis replicas relay-lote identity-larga)
preparar
echo "Pruebas de caos ($CID), cliente $CLIENTE, vehículo $VEHICULO"
for caso in "${CASOS[@]}"; do "caso_${caso//-/_}"; token; done
[ "$FALLAS" = 0 ] && echo "Todas las degradaciones se comportaron como estaba planificado." || echo "Hubo resultados distintos a lo planificado."
exit "$FALLAS"
