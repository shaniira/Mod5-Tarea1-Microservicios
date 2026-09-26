package com.andinaseguros.entities.model;

import static org.junit.jupiter.api.Assertions.*;

import com.andinaseguros.entities.enums.EstadoRenovacion;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.valueobject.Dinero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PropuestaRenovacionTest {
    @Test
    void approvesAndLinksGeneratedPolicy() {
        var now = LocalDateTime.now();
        var proposal = proposal(now, EstadoRenovacion.REQUIERE_RECALCULO);
        var policyId = UUID.randomUUID();

        proposal.aprobar(now);
        proposal.vincularPolizaRenovada(policyId);

        assertEquals(EstadoRenovacion.ACEPTADA, proposal.estado());
        assertEquals(policyId, proposal.polizaRenovadaId());
        assertNotNull(proposal.decididaEn());
    }

    @Test
    void rejectsDuplicateDecisionAndPolicyGenerationWithoutApproval() {
        var now = LocalDateTime.now();
        var rejected = proposal(now, EstadoRenovacion.AUTOMATICA);
        rejected.rechazar(now);

        assertThrows(ReglaNegocioException.class, () -> rejected.aprobar(now));
        assertThrows(
                ReglaNegocioException.class,
                () ->
                        proposal(now, EstadoRenovacion.EVALUACION_MANUAL)
                                .vincularPolizaRenovada(UUID.randomUUID()));
    }

    @Test
    void cannotApproveExpiredProposal() {
        var now = LocalDateTime.now();
        var expired =
                new PropuestaRenovacion(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Dinero.soles(new BigDecimal("1000")),
                        Dinero.soles(new BigDecimal("1080")),
                        new BigDecimal("8"),
                        1,
                        EstadoRenovacion.REQUIERE_RECALCULO,
                        "Prueba",
                        now.minusDays(40),
                        now.minusDays(10),
                        null,
                        null);

        assertThrows(ReglaNegocioException.class, () -> expired.aprobar(now));
        assertEquals(EstadoRenovacion.VENCIDA, expired.estado());
    }

    private PropuestaRenovacion proposal(LocalDateTime now, EstadoRenovacion state) {
        return new PropuestaRenovacion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Dinero.soles(new BigDecimal("1000")),
                Dinero.soles(new BigDecimal("1080")),
                new BigDecimal("8"),
                1,
                state,
                "Prueba",
                now,
                now.plusDays(30),
                null,
                null);
    }
}
