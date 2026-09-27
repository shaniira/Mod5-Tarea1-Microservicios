package com.andinaseguros.policy.usecases.service.renovacion;

import static com.andinaseguros.policy.usecases.mapper.PolizaResponseMapper.toResponse;

import com.andinaseguros.policy.entities.enums.EstadoPoliza;
import com.andinaseguros.policy.entities.enums.EstadoRenovacion;
import com.andinaseguros.policy.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.policy.entities.exception.ReglaNegocioException;
import com.andinaseguros.policy.entities.model.Poliza;
import com.andinaseguros.policy.entities.model.PropuestaRenovacion;
import com.andinaseguros.policy.entities.valueobject.PeriodoVigencia;
import com.andinaseguros.policy.usecases.dto.Responses.PolizaResponse;
import com.andinaseguros.policy.usecases.mapper.PolizaEventMapper;
import com.andinaseguros.policy.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.policy.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.policy.usecases.port.out.repository.RenovacionRepository;
import com.andinaseguros.policy.usecases.port.out.time.ClockPort;
import com.andinaseguros.policy.usecases.port.out.transaccion.TransaccionPort;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

/**
 * Mismas reglas que el monolito (propuesta aprobada, una sola póliza por propuesta, vigencia desde
 * el día siguiente al fin de la anterior). Además, la póliza nueva, la anterior (RENOVADA), la
 * propuesta y policy.renewed.v1 se guardan en una sola transacción (paso 6.6).
 */
public class GenerarPolizaRenovadaUseCase {
    private final RenovacionRepository renovacionRepository;
    private final PolizaRepository polizaRepository;
    private final DomainEventPublisherPort eventos;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public GenerarPolizaRenovadaUseCase(
            RenovacionRepository renovacionRepository,
            PolizaRepository polizaRepository,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.renovacionRepository = renovacionRepository;
        this.polizaRepository = polizaRepository;
        this.eventos = eventos;
        this.transaccion = transaccion;
        this.clock = clock;
        this.ids = ids;
    }

    public PolizaResponse execute(UUID renovacionId) {
        PropuestaRenovacion propuesta =
                renovacionRepository
                        .buscarPorId(renovacionId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Renovación"));
        if (propuesta.estado() != EstadoRenovacion.ACEPTADA) {
            throw new ReglaNegocioException(
                    "RENOVACION_NO_APROBADA", "Debe aprobar la propuesta antes de generar la póliza");
        }
        if (propuesta.polizaRenovadaId() != null) {
            return toResponse(
                    polizaRepository
                            .buscarPorId(propuesta.polizaRenovadaId())
                            .orElseThrow(() -> new RecursoNoEncontradoException("Póliza")));
        }
        Poliza origen =
                polizaRepository
                        .buscarPorId(propuesta.polizaOrigenId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Póliza de origen"));

        LocalDate inicio = origen.getVigencia().fin().plusDays(1);
        Poliza renovada =
                new Poliza(
                        ids.generar(),
                        "POL-REN-" + Year.from(inicio).getValue() + "-"
                                + ids.generar().toString().substring(0, 8).toUpperCase(),
                        null,
                        origen.getClienteId(),
                        origen.getVehiculoId(),
                        propuesta.nuevaPrima(),
                        new PeriodoVigencia(inicio, inicio.plusYears(1)),
                        EstadoPoliza.VIGENTE,
                        propuesta.id(),
                        1);
        origen.marcarRenovada();
        propuesta.vincularPolizaRenovada(renovada.getId());

        Poliza guardada =
                transaccion.ejecutar(
                        () -> {
                            Poliza nueva = polizaRepository.guardar(renovada);
                            Poliza anterior = polizaRepository.guardar(origen);
                            renovacionRepository.guardar(propuesta);
                            eventos.publicar(
                                    PolizaEventMapper.renovada(ids.generar(), clock.now(), nueva, anterior, propuesta.id()));
                            return nueva;
                        });
        return toResponse(guardada);
    }
}
