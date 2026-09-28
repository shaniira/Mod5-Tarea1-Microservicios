package com.backendseguros.customer.usecases.support;

import com.backendseguros.customer.usecases.port.out.transaccion.TransaccionPort;
import java.util.function.Supplier;

/** Ejecuta la operación sin transacción real y cuenta cuántas veces se usó. */
public class TransaccionDirecta implements TransaccionPort {
    private int usos;

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        usos++;
        return operacion.get();
    }

    public int usos() {
        return usos;
    }
}
