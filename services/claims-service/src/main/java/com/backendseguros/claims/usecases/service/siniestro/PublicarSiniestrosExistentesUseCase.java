package com.backendseguros.claims.usecases.service.siniestro;

import com.backendseguros.claims.entities.model.PolizaRef;
import com.backendseguros.claims.entities.model.Siniestro;
import com.backendseguros.claims.usecases.dto.Responses.ReenvioEventosResponse;
import com.backendseguros.claims.usecases.mapper.SiniestroEventMapper;
import com.backendseguros.claims.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.claims.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import com.backendseguros.claims.usecases.port.out.repository.SiniestroRepository;
import com.backendseguros.claims.usecases.port.out.time.ClockPort;

/**
 * Backfill: publica un claim.registered.v1 por cada siniestro, con su estado y su versión actuales.
 * Sirve para poblar la proyección open_claims_by_policy de policy-service (fase 6). Los
 * consumidores descartan las versiones que ya tienen, así que repetirlo no causa daño.
 */
public class PublicarSiniestrosExistentesUseCase {

    private final SiniestroRepository siniestros;
    private final PolizaRefRepository polizas;
    private final DomainEventPublisherPort eventos;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public PublicarSiniestrosExistentesUseCase(
            SiniestroRepository siniestros,
            PolizaRefRepository polizas,
            DomainEventPublisherPort eventos,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.siniestros = siniestros;
        this.polizas = polizas;
        this.eventos = eventos;
        this.clock = clock;
        this.ids = ids;
    }

    public ReenvioEventosResponse execute() {
        int publicados = 0;
        for (Siniestro siniestro : siniestros.listar()) {
            var clienteId =
                    polizas.buscarPorId(siniestro.polizaId()).map(PolizaRef::clienteId).orElse(null);
            eventos.publicar(
                    SiniestroEventMapper.registrado(ids.generar(), clock.now(), siniestro, clienteId));
            publicados++;
        }
        return new ReenvioEventosResponse(publicados);
    }
}
