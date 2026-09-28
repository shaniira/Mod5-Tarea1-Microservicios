package com.backendseguros.customer.usecases.service.placa;

import com.backendseguros.customer.entities.valueobject.Placa;
import com.backendseguros.customer.usecases.exception.VehicleProviderException;
import com.backendseguros.customer.usecases.model.VehicleInformation;
import com.backendseguros.customer.usecases.port.in.ConsultarInformacionVehiculoUseCase;
import com.backendseguros.customer.usecases.port.out.vehicle.VehicleInformationCachePort;
import com.backendseguros.customer.usecases.port.out.vehicle.VehicleInformationPort;
import java.util.Locale;
import java.util.Optional;

/**
 * Consulta de placas resiliente (paso 3.2). Primero la caché (TTL 24 h): una placa ya consultada
 * responde aunque JSON.pe esté caído y sin gastar otra llamada. Si no está, se consulta al
 * proveedor (con timeout, circuit breaker, reintento y bulkhead en el adaptador) y se guarda el
 * resultado. Si el proveedor falla o no tiene datos se devuelve SIN_DATOS: el frontend lo muestra
 * como "complete el registro manualmente", igual que en el monolito.
 */
public class ConsultarInformacionVehiculoService implements ConsultarInformacionVehiculoUseCase {
    public static final String SIN_DATOS = "SIN_DATOS";

    private final VehicleInformationPort vehicleInformationPort;
    private final VehicleInformationCachePort cache;

    public ConsultarInformacionVehiculoService(
            VehicleInformationPort vehicleInformationPort, VehicleInformationCachePort cache) {
        this.vehicleInformationPort = vehicleInformationPort;
        this.cache = cache;
    }

    @Override
    public VehicleInformation consultarPorPlaca(String placa) {
        String valorLimpio =
                placa == null
                        ? null
                        : placa.trim().replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        String normalizada = new Placa(valorLimpio).valor();

        Optional<VehicleInformation> enCache = cache.buscar(normalizada);
        if (enCache.isPresent()) {
            return enCache.get();
        }
        try {
            Optional<VehicleInformation> encontrada =
                    vehicleInformationPort.consultarPorPlaca(normalizada);
            encontrada.ifPresent(cache::guardar);
            return encontrada.orElseGet(() -> sinDatos(normalizada));
        } catch (VehicleProviderException exception) {
            return sinDatos(normalizada);
        }
    }

    private VehicleInformation sinDatos(String placa) {
        return new VehicleInformation(placa, null, null, null, null, null, null, SIN_DATOS);
    }
}
