package com.andinaseguros.usecases.service.siniestro;

import static com.andinaseguros.usecases.mapper.SiniestroResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.RegistrarSiniestroRequestModel;
import com.andinaseguros.usecases.dto.Responses.SiniestroResponse;
import com.andinaseguros.entities.enums.EstadoPoliza;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Poliza;
import com.andinaseguros.entities.model.Siniestro;
import com.andinaseguros.usecases.port.out.repository.PolizaRepository;
import com.andinaseguros.usecases.port.out.repository.SiniestroRepository;
import com.andinaseguros.entities.valueobject.Dinero;
import java.util.UUID;

public class RegistrarSiniestroUseCase {

    private final PolizaRepository polizaRepository;
    private final SiniestroRepository siniestroRepository;

    public RegistrarSiniestroUseCase(
            PolizaRepository polizaRepository, SiniestroRepository siniestroRepository) {
        this.polizaRepository = polizaRepository;
        this.siniestroRepository = siniestroRepository;
    }

    public SiniestroResponse execute(RegistrarSiniestroRequestModel solicitud) {
        Poliza poliza =
                polizaRepository
                        .buscarPorId(solicitud.polizaId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Póliza"));

        validarPolizaVigente(poliza);

        Siniestro siniestro =
                new Siniestro(
                        UUID.randomUUID(),
                        poliza.getId(),
                        solicitud.fecha(),
                        solicitud.tipo(),
                        Dinero.soles(solicitud.montoEstimado()),
                        solicitud.responsabilidadAsegurado(),
                        solicitud.gravedad(),
                        solicitud.estado());

        Siniestro siniestroGuardado = siniestroRepository.guardar(siniestro);

        return toResponse(siniestroGuardado);
    }

    private void validarPolizaVigente(Poliza poliza) {
        if (poliza.getEstado() != EstadoPoliza.VIGENTE) {
            throw new ReglaNegocioException(
                    "POLIZA_NO_VIGENTE", "Solo se registran siniestros en pólizas vigentes");
        }
    }
}
