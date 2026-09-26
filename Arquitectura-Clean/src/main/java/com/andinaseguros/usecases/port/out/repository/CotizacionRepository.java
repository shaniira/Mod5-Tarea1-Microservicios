package com.andinaseguros.usecases.port.out.repository;

import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.entities.model.Cotizacion;
import java.util.*;

public interface CotizacionRepository {
    Cotizacion guardar(Cotizacion cotizacion);

    Optional<Cotizacion> buscarPorId(UUID id);

    List<Cotizacion> listar();

    List<Cotizacion> listarPorEstado(EstadoCotizacion estado);

    List<Cotizacion> listarPorCliente(UUID clienteId);
}
