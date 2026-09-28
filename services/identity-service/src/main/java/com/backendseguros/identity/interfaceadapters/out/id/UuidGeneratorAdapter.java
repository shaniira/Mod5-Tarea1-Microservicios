package com.backendseguros.identity.interfaceadapters.out.id;

import com.backendseguros.identity.usecases.port.out.id.IdGeneratorPort;
import java.util.UUID;

public class UuidGeneratorAdapter implements IdGeneratorPort {
    public UUID generar() {
        return UUID.randomUUID();
    }
}
