package com.andinaseguros.quotation.interfaceadapters.out.id;

import com.andinaseguros.quotation.usecases.port.out.id.IdGeneratorPort;
import java.util.UUID;

public class UuidGeneratorAdapter implements IdGeneratorPort {
    public UUID generar() {
        return UUID.randomUUID();
    }
}
