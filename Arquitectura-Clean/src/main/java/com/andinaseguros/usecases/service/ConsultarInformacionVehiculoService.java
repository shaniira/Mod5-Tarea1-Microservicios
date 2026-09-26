package com.andinaseguros.usecases.service;

import com.andinaseguros.usecases.exception.VehicleProviderException;
import com.andinaseguros.usecases.model.VehicleInformation;
import com.andinaseguros.usecases.port.in.ConsultarInformacionVehiculoUseCase;
import com.andinaseguros.entities.valueobject.Placa;
import com.andinaseguros.usecases.port.out.vehicle.VehicleInformationPort;
import java.util.Locale;

public class ConsultarInformacionVehiculoService implements ConsultarInformacionVehiculoUseCase {
    private final VehicleInformationPort vehicleInformationPort;

    public ConsultarInformacionVehiculoService(VehicleInformationPort vehicleInformationPort) {
        this.vehicleInformationPort = vehicleInformationPort;
    }

    @Override
    public VehicleInformation consultarPorPlaca(String placa) {
        String valorLimpio =
                placa == null
                        ? null
                        : placa.trim().replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        String normalizada = new Placa(valorLimpio).valor();
        try {
            return vehicleInformationPort
                    .consultarPorPlaca(normalizada)
                    .orElseGet(() -> sinDatos(normalizada));
        } catch (VehicleProviderException exception) {
            return sinDatos(normalizada);
        }
    }

    private VehicleInformation sinDatos(String placa) {
        return new VehicleInformation(placa, null, null, null, null, null, null, "SIN_DATOS");
    }
}
