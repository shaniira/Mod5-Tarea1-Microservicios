package com.backendseguros.quotation.usecases.service.referencia;

import com.backendseguros.quotation.entities.model.ClienteRef;
import com.backendseguros.quotation.entities.model.VehiculoRef;
import com.backendseguros.quotation.usecases.port.out.repository.ReferenciaClientesRepository;

/**
 * Mantiene customer_ref y vehicle_ref con customer.registered/updated.v1 y vehicle.registered.v1
 * (paso 5.2). Los eventos repetidos o desordenados se descartan por la versión, así que no hace
 * falta una colección inbox.
 */
public class ActualizarReferenciasUseCase {
    public enum Resultado {
        APLICADO,
        VERSION_ANTIGUA
    }

    private final ReferenciaClientesRepository referencias;

    public ActualizarReferenciasUseCase(ReferenciaClientesRepository referencias) {
        this.referencias = referencias;
    }

    public Resultado cliente(ClienteRef cliente, long version) {
        return referencias.guardarClienteSiEsMasNuevo(cliente, version)
                ? Resultado.APLICADO
                : Resultado.VERSION_ANTIGUA;
    }

    public Resultado vehiculo(VehiculoRef vehiculo, long version) {
        return referencias.guardarVehiculoSiEsMasNuevo(vehiculo, version)
                ? Resultado.APLICADO
                : Resultado.VERSION_ANTIGUA;
    }
}
