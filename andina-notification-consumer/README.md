# Andina Notification Consumer

Sistema independiente que consume eventos `PolicyIssued` desde RabbitMQ y envia notificaciones por WhatsApp mediante JSON.pe. El proyecto ya contiene el consumidor funcional, la consulta de contactos en MongoDB, idempotencia, ACK, reintentos y DLQ.

## Flujo

```text
RabbitMQ -> RabbitMqNotificationConsumer -> MongoDB -> JSON.pe -> WhatsApp
```

El consumidor consulta el telefono del cliente usando `customerId`; el telefono no viaja dentro del evento de dominio.

## Ejecucion local

Requisitos: Java 21, Maven y RabbitMQ/MongoDB disponibles.

```bash
mvn spring-boot:run
```

Variables principales:

```text
SPRING_RABBITMQ_HOST
SPRING_RABBITMQ_PORT
SPRING_RABBITMQ_USERNAME
SPRING_RABBITMQ_PASSWORD
SPRING_DATA_MONGODB_URI
WHATSAPP_BASE_URL
WHATSAPP_TOKEN
```

## Docker

Desde la raiz (el Compose global usa la base de clientes de Hexagonal como fuente compartida):

```bash
docker compose up -d --build rabbitmq hexagonal-mongodb andina-notification-consumer
```

El consumidor no expone un puerto HTTP. Su estado operativo se observa en los logs del servicio.
