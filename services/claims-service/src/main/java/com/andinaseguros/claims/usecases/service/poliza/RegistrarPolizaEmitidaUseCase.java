package com.andinaseguros.claims.usecases.service.poliza;

import com.andinaseguros.claims.entities.enums.EstadoPoliza;
import com.andinaseguros.claims.entities.model.PolizaRef;
import com.andinaseguros.claims.usecases.port.out.repository.PolizaRefRepository;
import java.util.UUID;

/**
 * Mantiene policy_ref con policy.issued.v1 (paso 4.3). El monolito emite las pólizas en estado
 * VIGENTE (EmitirPolizaUseCase), así que eso es lo que se registra. Un evento repetido no cambia
 * nada, así que no hace falta una colección inbox.
 */
public class RegistrarPolizaEmitidaUseCase {
    public enum Resultado {
        REGISTRADA,
        YA_EXISTIA
    }

    private final PolizaRefRepository polizas;

    public RegistrarPolizaEmitidaUseCase(PolizaRefRepository polizas) {
        this.polizas = polizas;
    }

    public Resultado execute(UUID polizaId, UUID clienteId, String numero) {
        return polizas.registrarEmitidaSiNoExiste(
                        new PolizaRef(polizaId, clienteId, numero, EstadoPoliza.VIGENTE))
                ? Resultado.REGISTRADA
                : Resultado.YA_EXISTIA;
    }
}
