package com.andinaseguros.usecases.service.renovacion;

import static com.andinaseguros.usecases.mapper.RenovacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.Responses.RenovacionResponse;
import com.andinaseguros.entities.enums.EstadoPoliza;
import com.andinaseguros.entities.enums.EstadoRenovacion;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Poliza;
import com.andinaseguros.entities.model.PropuestaRenovacion;
import com.andinaseguros.entities.model.Siniestro;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.usecases.port.out.repository.RenovacionRepository;
import com.andinaseguros.usecases.port.out.repository.SiniestroRepository;
import com.andinaseguros.entities.service.CalculadorPrimaRenovacion;
import com.andinaseguros.entities.service.EvaluadorRenovacion;
import com.andinaseguros.entities.service.PoliticaVariacionPrima;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class EvaluarRenovacionUseCase {

    private final PolizaRepository polizaRepository;
    private final SiniestroRepository siniestroRepository;
    private final RenovacionRepository renovacionRepository;
    private final EvaluadorRenovacion evaluadorRenovacion;
    private final CalculadorPrimaRenovacion calculadorPrimaRenovacion;
    private final PoliticaVariacionPrima politicaVariacionPrima;

    public EvaluarRenovacionUseCase(
            PolizaRepository polizaRepository,
            SiniestroRepository siniestroRepository,
            RenovacionRepository renovacionRepository,
            EvaluadorRenovacion evaluadorRenovacion,
            CalculadorPrimaRenovacion calculadorPrimaRenovacion,
            PoliticaVariacionPrima politicaVariacionPrima) {
        this.polizaRepository = polizaRepository;
        this.siniestroRepository = siniestroRepository;
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

        validarPolizaVigente(poliza);

        List<Siniestro> historialSiniestros = siniestroRepository.listarPorPoliza(polizaId);
        EstadoRenovacion decision = evaluadorRenovacion.evaluar(historialSiniestros);

        int siniestrosResponsables =
                (int)
                        historialSiniestros.stream()
                                .filter(Siniestro::responsabilidadAsegurado)
                                .count();

        var calculoPrima =
                calculadorPrimaRenovacion.calcular(poliza.getPrima(), siniestrosResponsables);

        String motivo = politicaVariacionPrima.explicar(decision, siniestrosResponsables);

        LocalDateTime fechaEvaluacion = LocalDateTime.now();

        PropuestaRenovacion propuesta =
                new PropuestaRenovacion(
                        UUID.randomUUID(),
                        poliza.getId(),
                        poliza.getPrima(),
                        calculoPrima.nuevaPrima(),
                        calculoPrima.porcentajeVariacion(),
                        historialSiniestros.size(),
                        decision,
                        motivo,
                        fechaEvaluacion,
                        fechaEvaluacion.plusDays(30),
                        null,
                        null);

        PropuestaRenovacion propuestaGuardada = renovacionRepository.guardar(propuesta);

        return toResponse(propuestaGuardada);
    }

    private void validarPolizaVigente(Poliza poliza) {
        if (poliza.getEstado() != EstadoPoliza.VIGENTE) {
            throw new ReglaNegocioException(
                    "POLIZA_NO_VIGENTE", "Solo se pueden evaluar pólizas vigentes");
        }
    }
}
