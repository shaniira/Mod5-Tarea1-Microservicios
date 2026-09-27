package com.andinaseguros.customer.usecases.port.out.vehicle;

import com.andinaseguros.customer.usecases.model.VehicleInformation;
import java.util.Optional;

public interface VehicleInformationPort {
    Optional<VehicleInformation> consultarPorPlaca(String placa);
}
