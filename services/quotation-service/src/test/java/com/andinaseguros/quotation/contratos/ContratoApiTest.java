package com.andinaseguros.quotation.contratos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Contrato de la API (fase 7, paso 7.6): los controladores cumplen contracts/openapi/quotation-service.json.
 * Falla si se quita o renombra un endpoint, si una respuesta deja de traer un campo, si el cuerpo
 * pide un campo obligatorio nuevo o si hay un endpoint nuevo que todavía no está en el contrato.
 */
class ContratoApiTest {

    @Test
    void losControladoresCumplenElContratoOpenApi() {
        assertThat(Contratos.diferenciasApi("quotation-service.json", "com.andinaseguros.quotation.interfaceadapters.in.rest"))
                .isEmpty();
    }
}
