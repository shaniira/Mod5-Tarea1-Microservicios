package com.andinaseguros.customer.interfaceadapters.out.persistence.mongodb.adapter;

import com.andinaseguros.customer.usecases.port.out.transaccion.TransaccionPort;
import java.util.function.Supplier;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transacción multi-documento de MongoDB. Solo existe en un replica set (en Compose, uno de un
 * nodo). Si la base es un servidor suelto (por ejemplo el Mongo de Kubernetes, que aún no es
 * replica set), la operación se ejecuta sin transacción y se avisa una vez en el log: el Outbox
 * sigue evitando perder el evento si RabbitMQ está caído, pero ya no es atómico con el cambio.
 */
public class MongoTransaccionAdapter implements TransaccionPort {
    private static final Logger log = LoggerFactory.getLogger(MongoTransaccionAdapter.class);

    private final MongoTemplate mongoTemplate;
    private final TransactionTemplate transactionTemplate;
    private volatile Boolean replicaSet;

    public MongoTransaccionAdapter(
            MongoTemplate mongoTemplate, PlatformTransactionManager transactionManager) {
        this.mongoTemplate = mongoTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        if (!esReplicaSet()) {
            return operacion.get();
        }
        return transactionTemplate.execute(status -> operacion.get());
    }

    private boolean esReplicaSet() {
        Boolean valor = replicaSet;
        if (valor == null) {
            Document hello = mongoTemplate.getDb().runCommand(new Document("hello", 1));
            valor = hello.containsKey("setName");
            if (!valor) {
                log.warn(
                        "MongoDB no es un replica set: las operaciones con Outbox se ejecutan sin"
                                + " transaccion (el evento y el cambio de negocio no son atomicos)");
            }
            replicaSet = valor;
        }
        return valor;
    }
}
