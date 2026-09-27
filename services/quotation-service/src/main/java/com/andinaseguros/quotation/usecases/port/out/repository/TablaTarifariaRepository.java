package com.andinaseguros.quotation.usecases.port.out.repository;

import com.andinaseguros.quotation.entities.enums.*;
import com.andinaseguros.quotation.entities.model.TablaTarifaria;
import java.time.LocalDate;
import java.util.*;

public interface TablaTarifariaRepository {
    TablaTarifaria guardar(TablaTarifaria tablaTarifaria);

    Optional<TablaTarifaria> buscarPorId(UUID id);

    Optional<TablaTarifaria> buscarVigente(TipoVehiculo tipo, TipoUso uso, LocalDate fecha);

    List<TablaTarifaria> listar();
}
