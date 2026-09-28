package com.backendseguros.policy.usecases.service.referencia;

import com.backendseguros.policy.entities.model.CotizacionAceptada;
import com.backendseguros.policy.entities.model.SiniestroRef;
import com.backendseguros.policy.usecases.port.out.repository.CotizacionesAceptadasRepository;
import com.backendseguros.policy.usecases.port.out.repository.SiniestrosRefRepository;

/**
 * Mantiene accepted_quotes con quote.accepted.v1 y claim_ref con claim.registered/status-changed.v1
 * (paso 6.2). Los eventos repetidos no cambian nada (una cotización se acepta una sola vez; los
 * siniestros se comparan por versión), así que no hace falta una colección inbox.
 */
public class ActualizarProyeccionesUseCase {
    public enum Resultado {
        APLICADO,
        YA_EXISTIA,
        VERSION_ANTIGUA
    }

    private final CotizacionesAceptadasRepository cotizaciones;
    private final SiniestrosRefRepository siniestros;

    public ActualizarProyeccionesUseCase(
            CotizacionesAceptadasRepository cotizaciones, SiniestrosRefRepository siniestros) {
        this.cotizaciones = cotizaciones;
        this.siniestros = siniestros;
    }

    public Resultado cotizacionAceptada(CotizacionAceptada cotizacion) {
        return cotizaciones.registrarSiNoExiste(cotizacion) ? Resultado.APLICADO : Resultado.YA_EXISTIA;
    }

    public Resultado siniestro(SiniestroRef siniestro, long version) {
        return siniestros.guardarSiEsMasNuevo(siniestro, version) ? Resultado.APLICADO : Resultado.VERSION_ANTIGUA;
    }
}
