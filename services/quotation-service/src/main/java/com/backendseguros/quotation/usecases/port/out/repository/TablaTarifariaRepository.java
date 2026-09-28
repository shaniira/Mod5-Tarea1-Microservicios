package com.backendseguros.quotation.usecases.port.out.repository;

import com.backendseguros.quotation.entities.enums.*;
import com.backendseguros.quotation.entities.model.TablaTarifaria;
import java.time.LocalDate;
import java.util.*;

public interface TablaTarifariaRepository {
    TablaTarifaria guardar(TablaTarifaria tablaTarifaria);

    Optional<TablaTarifaria> buscarPorId(UUID id);

    Optional<TablaTarifaria> buscarVigente(TipoVehiculo tipo, TipoUso uso, LocalDate fecha);

    List<TablaTarifaria> listar();
}
