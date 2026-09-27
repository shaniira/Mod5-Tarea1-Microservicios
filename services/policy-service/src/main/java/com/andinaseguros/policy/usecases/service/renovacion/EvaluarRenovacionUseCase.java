package com.andinaseguros.policy.usecases.service.renovacion;

import static com.andinaseguros.policy.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.andinaseguros.policy.entities.enums.EstadoPoliza;
import com.andinaseguros.policy.entities.enums.EstadoRenovacion;
import com.andinaseguros.policy.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.policy.entities.exception.ReglaNegocioException;
import com.andinaseguros.policy.entities.model.Poliza;
import com.andinaseguros.policy.entities.model.PropuestaRenovacion;
import com.andinaseguros.policy.entities.model.SiniestroRef;
import com.andinaseguros.policy.entities.service.CalculadorPrimaRenovacion;
import com.andinaseguros.policy.entities.service.EvaluadorRenovacion;
import com.andinaseguros.policy.entities.service.PoliticaVariacionPrima;
import com.andinaseguros.policy.usecases.dto.Responses.RenovacionResponse;
import com.andinaseguros.policy.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.policy.usecases.port.out.repository.RenovacionRepository;
import com.andinaseguros.policy.usecases.port.out.repository.SincronizacionSiniestrosPort;
import com.andinaseguros.policy.usecases.port.out.repository.SiniestrosRefRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Misma evaluación que el monolito (paso 6.5), pero el historial de siniestros sale de la
 * proyección claim_ref, no de la base de siniestros. Si la proyección no está al día (sin carga
 * inicial o con eventos de siniestros todavía por aplicar), no se puede saber si hay siniestros
 * abiertos: se trata como "hay pendientes" y se pide reintentar, en lugar de arriesgarse a renovar
 * una póliza con un siniestro abierto.
 */
public class EvaluarRenovacionUseCase {
    private final PolizaRepository polizaRepository;
    private final SiniestrosRefRepository siniestros;
    private final SincronizacionSiniestrosPort sincronizacion;
    private final RenovacionRepository renovacionRepository;
    private final EvaluadorRenovacion evaluadorRenovacion;
    private final CalculadorPrimaRenovacion calculadorPrimaRenovacion;
    private final PoliticaVariacionPrima politicaVariacionPrima;

    public EvaluarRenovacionUseCase(
            PolizaRepository polizaRepository,
            SiniestrosRefRepository siniestros,
            SincronizacionSiniestrosPort sincronizacion,
            RenovacionRepository renovacionRepository,
            EvaluadorRenovacion evaluadorRenovacion,
            CalculadorPrimaRenovacion calculadorPrimaRenovacion,
            PoliticaVariacionPrima politicaVariacionPrima) {
        this.polizaRepository = polizaRepository;
        this.siniestros = siniestros;
        this.sincronizacion = sincronizacion;
        this.renovacionRepository = renovacionRepository;
        this.evaluadorRenovacion = evaluadorRenovacion;
        this.calculadorPrimaRenovacion = calculadorPrimaRenovacion;
        this.politicaVariacionPrima = politicaVariacionPrima;
    }

    public RenovacionResponse execute(UUID polizaId) {
        Poliza poliza =
                polizaRepository
                        .buscarPorId(polizaId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Póliza"));
        if (poliza.getEstado() != EstadoPoliza.VIGENTE) {
            throw new ReglaNegocioException("POLIZA_NO_VIGENTE", "Solo se pueden evaluar pólizas vigentes");
        }
        if (!sincronizacion.estaAlDia()) {
            throw new ReglaNegocioException(
                    "SINIESTROS_PENDIENTES",
                    "Aún no se puede confirmar el historial de siniestros de la póliza. Intenta de nuevo más tarde.");
        }

        List<SiniestroRef> historial = siniestros.listarPorPoliza(polizaId);
        EstadoRenovacion decision = evaluadorRenovacion.evaluar(historial);
        int responsables = (int) historial.stream().filter(SiniestroRef::responsabilidadAsegurado).count();
        var calculoPrima = calculadorPrimaRenovacion.calcular(poliza.getPrima(), responsables);
        String motivo = politicaVariacionPrima.explicar(decision, responsables);

        LocalDateTime fechaEvaluacion = LocalDateTime.now();
        PropuestaRenovacion propuesta =
                new PropuestaRenovacion(
                        UUID.randomUUID(),
                        poliza.getId(),
                        poliza.getPrima(),
                        calculoPrima.nuevaPrima(),
                        calculoPrima.porcentajeVariacion(),
                        historial.size(),
                        decision,
                        motivo,
                        fechaEvaluacion,
                        fechaEvaluacion.plusDays(30),
                        null,
                        null);
        return toResponse(renovacionRepository.guardar(propuesta));
    }
}
