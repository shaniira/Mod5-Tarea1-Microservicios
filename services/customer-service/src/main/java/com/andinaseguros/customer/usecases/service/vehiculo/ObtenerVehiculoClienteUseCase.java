package com.andinaseguros.customer.usecases.service.vehiculo;

import static com.andinaseguros.customer.usecases.mapper.VehiculoResponseMapper.toResponse;

import com.andinaseguros.customer.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.customer.usecases.dto.Responses.VehiculoResponse;
import com.andinaseguros.customer.usecases.port.out.repository.VehiculoRepository;
import java.util.UUID;

/**
 * GET /api/clientes/{id}/vehiculos/{vehiculoId}: lectura de refuerzo (read-through) que usará
 * quotation-service cuando un vehículo aún no esté en su proyección (sección 5.4 de la propuesta).
 * Un vehículo de otro cliente se responde como no encontrado.
 */
public class ObtenerVehiculoClienteUseCase {
    private final VehiculoRepository vehiculoRepository;

    public ObtenerVehiculoClienteUseCase(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    public VehiculoResponse execute(UUID clienteId, UUID vehiculoId) {
        return vehiculoRepository
                .buscarPorId(vehiculoId)
                .filter(vehiculo -> vehiculo.getClienteId().equals(clienteId))
                .map(vehiculo -> toResponse(vehiculo))
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo"));
    }
}
