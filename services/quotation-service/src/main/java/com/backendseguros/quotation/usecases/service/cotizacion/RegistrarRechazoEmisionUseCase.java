package com.backendseguros.quotation.usecases.service.cotizacion;

import com.backendseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import com.backendseguros.quotation.usecases.port.out.repository.RechazoEmisionRepository;
import java.util.UUID;

/**
 * Compensación de la saga de emisión (paso 6.4): policy-service no emitió la póliza
 * (policy.issuance-rejected.v1). La cotización queda ACEPTADA (quotation nunca la marca EMITIDA
 * sin policy.issued.v1) y se registra el motivo para que el agente lo vea.
 */
public class RegistrarRechazoEmisionUseCase {
    public enum Resultado {
        REGISTRADO,
        NO_EXISTE
    }

    private final CotizacionRepository cotizaciones;
    private final RechazoEmisionRepository rechazos;

    public RegistrarRechazoEmisionUseCase(CotizacionRepository cotizaciones, RechazoEmisionRepository rechazos) {
        this.cotizaciones = cotizaciones;
        this.rechazos = rechazos;
    }

    public Resultado execute(UUID eventId, UUID cotizacionId, String codigo, String motivo) {
        var cotizacion = cotizaciones.buscarPorId(cotizacionId);
        if (cotizacion.isEmpty()) {
            return Resultado.NO_EXISTE;
        }
        rechazos.registrar(eventId, cotizacionId, codigo, motivo);
        return Resultado.REGISTRADO;
    }
}
