package com.andinaseguros.claims.usecases.port.out.repository;

import com.andinaseguros.claims.entities.enums.EstadoPoliza;
import com.andinaseguros.claims.entities.model.PolizaRef;
import java.util.Optional;
import java.util.UUID;

/** Proyección policy_ref (paso 4.3): reemplaza a PolizaRepository del monolito. */
public interface PolizaRefRepository {
    Optional<PolizaRef> buscarPorId(UUID polizaId);

    /**
     * Registra una póliza recién emitida (policy.issued.v1 o la nueva de policy.renewed.v1) solo si
     * no existe todavía: la emisión es el primer estado de una póliza, así que un evento repetido o
     * atrasado nunca pisa un estado posterior. Devuelve false si ya existía.
     */
    boolean registrarEmitidaSiNoExiste(PolizaRef poliza);

    /**
     * Cambia el estado (RENOVADA, VENCIDA, CANCELADA) si la versión que llega es mayor que la
     * guardada. Las pólizas de la carga inicial o de eventos del backend no tienen versión y cuentan
     * como 0. Devuelve false si el evento era viejo o la póliza no está en la proyección.
     */
    boolean actualizarEstadoSiEsMasNuevo(UUID polizaId, EstadoPoliza estado, long version);
}
