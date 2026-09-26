package com.andinaseguros.interfaceadapters.out.external.jsonpe;

import com.andinaseguros.usecases.model.VehicleInformation;
import com.andinaseguros.usecases.port.out.vehicle.*;
import java.util.Optional;

public class JsonPeVehicleInformationAdapter implements VehicleInformationPort {
    private final JsonPeClient client;
    private final JsonPeVehicleMapper mapper;

    public JsonPeVehicleInformationAdapter(JsonPeClient client, JsonPeVehicleMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public Optional<VehicleInformation> consultarPorPlaca(String placa) {
        JsonPePlateResponse response = client.consultar(placa);
        if (response == null || !response.success() || response.data() == null)
            return Optional.empty();
        return Optional.of(mapper.toDomain(response));
    }
}
