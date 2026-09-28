package com.backendseguros.customer.usecases.port.out.transaccion;

import java.util.function.Supplier;

/**
 * Ejecuta una operación como una sola unidad: el cambio de negocio y el evento que se guarda en
 * el Outbox se confirman juntos o no se confirma ninguno.
 */
public interface TransaccionPort {
    <T> T ejecutar(Supplier<T> operacion);
}
