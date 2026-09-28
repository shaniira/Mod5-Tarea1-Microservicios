package com.backendseguros.customer.interfaceadapters.out.external.jsonpe;

import com.backendseguros.customer.usecases.exception.VehicleProviderException;
import com.backendseguros.customer.usecases.exception.VehicleProviderUnavailableException;
import com.backendseguros.customer.usecases.model.VehicleInformation;
import com.backendseguros.customer.usecases.port.out.vehicle.VehicleInformationPort;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consulta de placas en JSON.pe con los patrones del paso 3.2 (tabla 6.2 de la propuesta): el
 * timeout de 5 s está en el RestClient; aquí se agregan un reintento con espera aleatoria, el
 * circuit breaker "jsonpe" y un bulkhead que limita las llamadas simultáneas. El orden es
 * bulkhead → circuit breaker → retry → llamada: si el circuito está abierto no se reintenta ni se
 * ocupa un cupo esperando.
 *
 * <p>Solo cuentan para el circuito las caídas del proveedor (sin respuesta, timeout, 5xx). Un token
 * rechazado o una placa inválida son errores de configuración o de datos, no de disponibilidad.
 */
public class JsonPeVehicleInformationAdapter implements VehicleInformationPort {
    private static final Logger log = LoggerFactory.getLogger(JsonPeVehicleInformationAdapter.class);

    private final JsonPeClient client;
    private final JsonPeVehicleMapper mapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;

    public JsonPeVehicleInformationAdapter(
            JsonPeClient client,
            JsonPeVehicleMapper mapper,
            CircuitBreaker circuitBreaker,
            Retry retry,
            Bulkhead bulkhead) {
        this.client = client;
        this.mapper = mapper;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.bulkhead = bulkhead;
    }

    @Override
    public Optional<VehicleInformation> consultarPorPlaca(String placa) {
        Supplier<JsonPePlateResponse> llamada = () -> client.consultar(placa);
        Supplier<JsonPePlateResponse> protegida =
                Bulkhead.decorateSupplier(
                        bulkhead,
                        CircuitBreaker.decorateSupplier(
                                circuitBreaker, Retry.decorateSupplier(retry, llamada)));
        JsonPePlateResponse response;
        try {
            response = protegida.get();
        } catch (CallNotPermittedException | BulkheadFullException sinCupo) {
            log.warn("JSON.pe no consultado ({}): circuito abierto o sin cupo", sinCupo.getClass().getSimpleName());
            throw new VehicleProviderUnavailableException("JSONPE_NO_DISPONIBLE", sinCupo);
        } catch (VehicleProviderException fallo) {
            // El caso de uso responde SIN_DATOS (ingreso manual); aquí queda el motivo en el log.
            log.warn("JSON.pe falló al consultar la placa {}: {} ({})", placa, fallo.getMessage(), causaRaiz(fallo));
            throw fallo;
        }
        if (response == null || !response.success() || response.data() == null) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(response));
    }

    private static String causaRaiz(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }
}
