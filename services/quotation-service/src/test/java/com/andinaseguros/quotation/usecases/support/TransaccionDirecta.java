package com.andinaseguros.quotation.usecases.support;

import com.andinaseguros.quotation.usecases.port.out.transaccion.TransaccionPort;
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
