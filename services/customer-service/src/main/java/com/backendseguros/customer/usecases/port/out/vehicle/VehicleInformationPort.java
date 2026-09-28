package com.backendseguros.customer.usecases.port.out.vehicle;

import com.backendseguros.customer.usecases.model.VehicleInformation;
import java.util.Optional;

public interface VehicleInformationPort {
    Optional<VehicleInformation> consultarPorPlaca(String placa);
}
