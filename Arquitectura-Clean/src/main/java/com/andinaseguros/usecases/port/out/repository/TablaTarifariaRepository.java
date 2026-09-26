package com.andinaseguros.usecases.port.out.repository;

import com.andinaseguros.entities.enums.*;
import com.andinaseguros.entities.model.TablaTarifaria;
import java.time.LocalDate;
import java.util.*;

public interface TablaTarifariaRepository {
    TablaTarifaria guardar(TablaTarifaria tablaTarifaria);

    Optional<TablaTarifaria> buscarPorId(UUID id);

    Optional<TablaTarifaria> buscarVigente(TipoVehiculo tipo, TipoUso uso, LocalDate fecha);

    List<TablaTarifaria> listar();
}
