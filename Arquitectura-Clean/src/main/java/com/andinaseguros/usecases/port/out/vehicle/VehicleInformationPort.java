package com.andinaseguros.usecases.port.out.vehicle;

import com.andinaseguros.usecases.model.VehicleInformation;
import java.util.Optional;

public interface VehicleInformationPort {
    Optional<VehicleInformation> consultarPorPlaca(String placa);
}
