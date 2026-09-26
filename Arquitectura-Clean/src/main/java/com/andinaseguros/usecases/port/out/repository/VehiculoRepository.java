package com.andinaseguros.usecases.port.out.repository;

import com.andinaseguros.entities.model.Vehiculo;
import java.util.*;

public interface VehiculoRepository {
    Vehiculo guardar(Vehiculo vehiculo);

    Optional<Vehiculo> buscarPorId(UUID id);

    Optional<Vehiculo> buscarPorPlaca(String placa);

    List<Vehiculo> listarPorCliente(UUID clienteId);
}
