package com.backendseguros.quotation.usecases.service.cotizacion;

import static com.backendseguros.quotation.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.backendseguros.quotation.entities.event.CotizacionAceptadaEvent;
import com.backendseguros.quotation.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.quotation.entities.model.Cotizacion;
import com.backendseguros.quotation.usecases.dto.Responses.CotizacionResponse;
import com.backendseguros.quotation.usecases.port.out.event.DomainEventPublisherPort;
import com.backendseguros.quotation.usecases.port.out.id.IdGeneratorPort;
import com.backendseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import com.backendseguros.quotation.usecases.port.out.time.ClockPort;
import com.backendseguros.quotation.usecases.port.out.transaccion.TransaccionPort;
import java.util.UUID;

/**
 * Acepta una cotización vigente (misma regla que el monolito) y publica quote.accepted.v1 en la
 * misma transacción (paso 5.4, Outbox): la cotización aceptada y su evento se guardan juntos.
 */
public class AceptarCotizacionUseCase {
    private final CotizacionRepository cotizaciones;
    private final DomainEventPublisherPort eventos;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public AceptarCotizacionUseCase(
            CotizacionRepository cotizaciones,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.cotizaciones = cotizaciones;
        this.eventos = eventos;
        this.transaccion = transaccion;
        this.clock = clock;
        this.ids = ids;
    }

    public CotizacionResponse execute(UUID cotizacionId) {
        Cotizacion cotizacion =
                cotizaciones
                        .buscarPorId(cotizacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cotización"));
        cotizacion.aceptar();

        Cotizacion guardada =
                transaccion.ejecutar(
                        () -> {
                            Cotizacion resultado = cotizaciones.guardar(cotizacion);
                            eventos.publicar(
                                    new CotizacionAceptadaEvent(
                                            ids.generar(),
                                            clock.now(),
                                            resultado.getId(),
                                            resultado.getNumero(),
                                            resultado.getClienteId(),
                                            resultado.getVehiculoId(),
                                            resultado.getPrima().valor(),
                                            resultado.getPrima().moneda(),
                                            resultado.getTablaTarifariaId(),
                                            resultado.getFechaCreacion(),
                                            resultado.getFechaExpiracion(),
                                            resultado.getDesglose()));
                            return resultado;
                        });
        return toResponse(guardada, null);
    }
}
