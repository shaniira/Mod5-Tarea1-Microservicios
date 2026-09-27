package com.andinaseguros.claims.interfaceadapters.out.id;

import com.andinaseguros.claims.usecases.port.out.id.IdGeneratorPort;
import java.util.UUID;

public class UuidGeneratorAdapter implements IdGeneratorPort {
    public UUID generar() {
        return UUID.randomUUID();
    }
}
