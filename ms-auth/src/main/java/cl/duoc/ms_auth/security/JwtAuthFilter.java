package cl.duoc.ms_auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.stream.Collectors;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtUtils jwtUtils) { this.jwtUtils = jwtUtils; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        logger.debug("Procesando solicitud en JwtAuthFilter.");

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            logger.debug("Token extraído de la cabecera de autorización.");

            jwtUtils.validateAndParse(token).ifPresent(payload -> {
                logger.debug("Token JWT válido. Estableciendo contexto de seguridad para el usuario: {}", payload.username());
                var authorities = payload.roles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toSet());

                var authentication = new UsernamePasswordAuthenticationToken(
                        payload.username(), null, authorities
                );

                authentication.setDetails(payload);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Contexto de seguridad establecido para el usuario: {}", payload.username());
            });
        } else {
            logger.debug("No se encontró un token JWT válido en la cabecera de autorización.");
        }

        chain.doFilter(req, res);
    }
}
