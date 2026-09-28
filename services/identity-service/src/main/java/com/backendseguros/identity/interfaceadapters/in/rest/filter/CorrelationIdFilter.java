package com.backendseguros.identity.interfaceadapters.in.rest.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Toma el X-Correlation-Id que genera el gateway (o crea uno si la petición llega directo) y lo deja
 * en el MDC: aparece en el log y el Outbox lo copia a los eventos, así el hilo sigue hasta
 * notification-service.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String received = request.getHeader(HEADER);
        boolean valido = received != null && VALID.matcher(received).matches();
        String correlationId = valido ? received : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, correlationId);
        if (!valido) {
            // Si llegó del gateway, el gateway ya lo devuelve en la respuesta; repetirlo aquí
            // duplicaría el encabezado.
            response.setHeader(HEADER, correlationId);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
