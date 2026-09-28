package com.backendseguros.claims.usecases.service.poliza;

import com.backendseguros.claims.entities.enums.EstadoPoliza;
import com.backendseguros.claims.entities.model.PolizaRef;
import com.backendseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import java.util.UUID;

/**
 * Mantiene policy_ref con los cambios de estado que publica policy-service (fase 6):
 * policy.renewed.v1 (la anterior pasa a RENOVADA y la nueva queda VIGENTE), policy.expired.v1 y
 * policy.cancelled.v1. Con esto policy_ref ya no depende de repetir el script de carga inicial.
 */
public class ActualizarEstadoPolizaUseCase {
    public enum Resultado {
        APLICADO,
        IGNORADO
    }

    private final PolizaRefRepository polizas;

    public ActualizarEstadoPolizaUseCase(PolizaRefRepository polizas) {
        this.polizas = polizas;
    }

    public Resultado renovada(
            UUID polizaAnteriorId, long versionAnterior, UUID polizaNuevaId, UUID clienteId, String numeroNueva) {
        polizas.registrarEmitidaSiNoExiste(new PolizaRef(polizaNuevaId, clienteId, numeroNueva, EstadoPoliza.VIGENTE));
        return cambiarEstado(polizaAnteriorId, EstadoPoliza.RENOVADA, versionAnterior);
    }

    public Resultado cambiarEstado(UUID polizaId, EstadoPoliza estado, long version) {
        return polizas.actualizarEstadoSiEsMasNuevo(polizaId, estado, version) ? Resultado.APLICADO : Resultado.IGNORADO;
    }
}
