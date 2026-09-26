package com.andinaseguros.usecases.service.poliza;

import static com.andinaseguros.usecases.mapper.PolizaResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.EmitirPolizaRequestModel;
import com.andinaseguros.usecases.dto.Responses.PolizaResponse;
import com.andinaseguros.usecases.port.in.EmitirPolizaInputPort;
import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.entities.enums.EstadoPoliza;
import com.andinaseguros.entities.event.PolizaEmitidaEvent;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Cotizacion;
import com.andinaseguros.entities.model.Poliza;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.entities.valueobject.PeriodoVigencia;
import com.andinaseguros.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.usecases.port.out.time.ClockPort;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;

public class EmitirPolizaUseCase implements EmitirPolizaInputPort {

    private final CotizacionRepository cotizacionRepository;
    private final PolizaRepository polizaRepository;
    private final DomainEventPublisherPort eventPublisher;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public EmitirPolizaUseCase(
            CotizacionRepository cotizacionRepository,
            PolizaRepository polizaRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.cotizacionRepository = cotizacionRepository;
        this.polizaRepository = polizaRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.ids = ids;
    }

    @Override
    public PolizaResponse execute(EmitirPolizaRequestModel solicitud) {
        Cotizacion cotizacion =
                cotizacionRepository
                        .buscarPorId(solicitud.cotizacionId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cotización"));

        validarCotizacionAceptada(cotizacion);
        if (polizaRepository.buscarPorCotizacionId(cotizacion.getId()).isPresent()) {
            throw new ReglaNegocioException(
                    "COTIZACION_YA_EMITIDA", "La cotización ya tiene una póliza emitida");
        }
        cotizacion.marcarEmitida();

        LocalDate inicioVigencia = solicitud.inicioVigencia();
        String numeroPoliza =
                "POL-"
                        + Year.from(clock.now().atZone(ZoneOffset.UTC)).getValue()
                        + "-"
                        + ids.generar().toString().substring(0, 8).toUpperCase();

        Poliza poliza =
                new Poliza(
                        ids.generar(),
                        numeroPoliza,
                        cotizacion.getId(),
                        cotizacion.getClienteId(),
                        cotizacion.getVehiculoId(),
                        cotizacion.getPrima(),
                        new PeriodoVigencia(inicioVigencia, inicioVigencia.plusYears(1)),
                        EstadoPoliza.VIGENTE);

        cotizacionRepository.guardar(cotizacion);
        Poliza polizaGuardada = polizaRepository.guardar(poliza);
        eventPublisher.publicar(
                new PolizaEmitidaEvent(
                        ids.generar(),
                        clock.now(),
                        polizaGuardada.getId(),
                        cotizacion.getId(),
                        cotizacion.getClienteId(),
                        polizaGuardada.getNumero()));

        return toResponse(polizaGuardada);
    }

    private void validarCotizacionAceptada(Cotizacion cotizacion) {
        if (cotizacion.getEstado() != EstadoCotizacion.ACEPTADA) {
            throw new ReglaNegocioException(
                    "COTIZACION_NO_ACEPTADA",
                    "La cotización debe estar aceptada antes de emitir la póliza");
        }
    }
}
