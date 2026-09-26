package com.andinaseguros.interfaceadapters.out.security;

import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.usecases.port.out.security.TokenValidationPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenValidationPort tokenValidation;
    private final UsuarioRepository usuarios;

    public JwtAuthenticationFilter(
            TokenValidationPort tokenValidation, UsuarioRepository usuarios) {
        this.tokenValidation = tokenValidation;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7);
            var claims = tokenValidation.validar(token);

            var usuario =
                    usuarios.buscarPorUsername(claims.username())
                            .filter(u -> u.isActivo())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Usuario inactivo o inexistente"));

            var authority = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name());
            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            usuario.getUsername(), null, List.of(authority));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter()
                    .write(
                            "{\"codigo\":\"TOKEN_INVALIDO\",\"message\":\"La sesión expiró o el"
                                    + " token no es válido. Inicie sesión nuevamente.\"}");
        }
    }
}
