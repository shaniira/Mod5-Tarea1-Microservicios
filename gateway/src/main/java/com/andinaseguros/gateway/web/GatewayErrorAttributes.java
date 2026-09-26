package com.andinaseguros.gateway.web;

import com.andinaseguros.gateway.filter.CorrelationIdGlobalFilter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * Manejo centralizado de errores del Gateway: toda respuesta de error que no generan los
 * filtros propios (401 de {@code JwtAuthenticationGlobalFilter}, 503 de
 * {@code FallbackController}) pasa por aqui con la misma forma de JSON y el mismo
 * {@code correlationId}, en vez del cuerpo de error por defecto de Spring Boot.
 */
@Component
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> defaults = super.getErrorAttributes(request, options);
        int status =
                defaults.get("status") instanceof Integer i
                        ? i
                        : HttpStatus.INTERNAL_SERVER_ERROR.value();

        String correlationId =
                (String)
                        request.exchange()
                                .getAttributes()
                                .get(CorrelationIdGlobalFilter.CORRELATION_ID_ATTRIBUTE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        body.put("message", defaults.getOrDefault("message", ""));
        body.put("path", request.path());
        body.put("correlationId", correlationId);
        return body;
    }
}
