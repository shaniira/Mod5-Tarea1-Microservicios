#!/bin/sh
# Reproceso de una DLQ (paso 7.7 de la ruta): mueve sus mensajes de vuelta a la cola principal con
# un shovel de RabbitMQ que se borra solo al vaciar la DLQ. Los consumidores son idempotentes: un
# mensaje que ya se había aplicado no se aplica dos veces.
#
# Uso (desde la raíz del repositorio, con el stack de Compose levantado):
#   sh infra/rabbitmq/reprocesar-dlq.sh <dlq> [cola-destino]
#   sh infra/rabbitmq/reprocesar-dlq.sh notification.policy.events.dlq
#
# La cola destino por defecto es el nombre de la DLQ sin ".dlq" (convención de todas las colas).
# Antes de reprocesar hay que corregir la causa (ver la guía de operación, doc/5. Microservicios/
# o_GUIA-OPERACION.md); si no, los mensajes vuelven a la DLQ.
# En Kubernetes: los mismos comandos rabbitmqctl con "kubectl exec -n andina-seguros deploy/rabbitmq --".
set -eu

DLQ="${1:?Uso: reprocesar-dlq.sh <dlq> [cola-destino]}"
DESTINO="${2:-${DLQ%.dlq}}"
CONTENEDOR="${RABBITMQ_CONTAINER:-andina-clean-rabbitmq-1}"
SHOVEL="reproceso-$DLQ"

mensajes() {
  docker exec "$CONTENEDOR" rabbitmqctl -q list_queues name messages | awk -v q="$1" '$1 == q { print $2 }'
}

EN_DLQ="$(mensajes "$DLQ")"
[ -n "$EN_DLQ" ] || { echo "No existe la cola $DLQ" >&2; exit 1; }
[ -n "$(mensajes "$DESTINO")" ] || { echo "No existe la cola destino $DESTINO" >&2; exit 1; }
if [ "$EN_DLQ" = "0" ]; then
  echo "$DLQ está vacía: no hay nada que reprocesar"
  exit 0
fi

echo "Moviendo $EN_DLQ mensaje(s) de $DLQ a $DESTINO"
docker exec "$CONTENEDOR" rabbitmqctl -q set_parameter shovel "$SHOVEL" \
  "{\"src-protocol\":\"amqp091\",\"src-uri\":\"amqp://\",\"src-queue\":\"$DLQ\",\"dest-protocol\":\"amqp091\",\"dest-uri\":\"amqp://\",\"dest-queue\":\"$DESTINO\",\"src-delete-after\":\"queue-length\"}"

# El shovel se borra solo al terminar; se espera hasta 60 s.
i=0
while docker exec "$CONTENEDOR" rabbitmqctl -q list_parameters -p / 2>/dev/null | grep -q "$SHOVEL"; do
  i=$((i + 1))
  [ "$i" -le 60 ] || { echo "El shovel $SHOVEL no terminó en 60 s; revisar en la consola" >&2; exit 1; }
  sleep 1
done
echo "Listo. Mensajes que quedan en $DLQ: $(mensajes "$DLQ")"
