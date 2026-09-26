package com.andinaseguros.usecases.service.vehiculo;

import static com.andinaseguros.usecases.mapper.VehiculoResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.CrearVehiculoRequestModel;
import com.andinaseguros.usecases.dto.Responses.VehiculoResponse;
import com.andinaseguros.usecases.port.in.RegistrarVehiculoUseCase;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Vehiculo;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.repository.VehiculoRepository;
import com.andinaseguros.entities.valueobject.Placa;
import java.util.UUID;

public class CrearVehiculoUseCase implements RegistrarVehiculoUseCase {

    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;

    public CrearVehiculoUseCase(
            ClienteRepository clienteRepository, VehiculoRepository vehiculoRepository) {
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    @Override
    public VehiculoResponse execute(CrearVehiculoRequestModel solicitud) {
        validarCliente(solicitud.clienteId());
        validarPlacaDisponible(solicitud.placa());
        Vehiculo vehiculo =
                new Vehiculo(
                        UUID.randomUUID(),
                        solicitud.clienteId(),
                        new Placa(solicitud.placa()),
                        solicitud.marca(),
                        solicitud.modelo(),
                        solicitud.anioFabricacion(),
                        solicitud.tipo(),
                        solicitud.uso(),
                        solicitud.zonaCirculacion());

        Vehiculo vehiculoGuardado = vehiculoRepository.guardar(vehiculo);

        return toResponse(vehiculoGuardado);
    }

    private void validarCliente(UUID clienteId) {
        clienteRepository
                .buscarPorId(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente"));
    }

    private void validarPlacaDisponible(String placa) {
        if (vehiculoRepository.buscarPorPlaca(placa).isPresent()) {
            throw new ReglaNegocioException("PLACA_DUPLICADA", "La placa ya está registrada");
        }
    }
}
