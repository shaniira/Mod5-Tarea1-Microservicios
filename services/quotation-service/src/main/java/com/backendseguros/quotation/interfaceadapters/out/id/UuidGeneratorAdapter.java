package com.backendseguros.quotation.interfaceadapters.out.id;

import com.backendseguros.quotation.usecases.port.out.id.IdGeneratorPort;
import java.util.UUID;

public class UuidGeneratorAdapter implements IdGeneratorPort {
    public UUID generar() {
        return UUID.randomUUID();
    }
}
