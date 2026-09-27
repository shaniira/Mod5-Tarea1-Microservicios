package com.andinaseguros.customer.usecases.port.out.repository;

import com.andinaseguros.customer.entities.model.Vehiculo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehiculoRepository {
    Vehiculo guardar(Vehiculo vehiculo);

    Optional<Vehiculo> buscarPorId(UUID id);

    Optional<Vehiculo> buscarPorPlaca(String placa);

    List<Vehiculo> listarPorCliente(UUID clienteId);

    List<Vehiculo> listar();
}
