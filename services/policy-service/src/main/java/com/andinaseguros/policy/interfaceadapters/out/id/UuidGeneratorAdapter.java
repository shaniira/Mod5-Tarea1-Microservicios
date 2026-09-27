package com.andinaseguros.policy.interfaceadapters.out.id;

import com.andinaseguros.policy.usecases.port.out.id.IdGeneratorPort;
import java.util.UUID;

public class UuidGeneratorAdapter implements IdGeneratorPort {
    public UUID generar() {
        return UUID.randomUUID();
    }
}
