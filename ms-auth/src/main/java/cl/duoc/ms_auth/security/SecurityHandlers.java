package cl.duoc.ms_auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.Map;

@Configuration
public class SecurityHandlers {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHandlers.class);

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper om) {
        return (request, response, authException) -> {
            logger.debug("Acceso no autenticado denegado para la ruta: {}. Causa: {}", request.getRequestURI(), authException.getMessage());
            writeJson(response, om, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper om) {
        return (request, response, accessDeniedException) -> {
            logger.debug("Acceso prohibido para la ruta: {}. Causa: {}", request.getRequestURI(), accessDeniedException.getMessage());
            writeJson(response, om, HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        };
    }

    private void writeJson(HttpServletResponse res, ObjectMapper om, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        om.writeValue(res.getWriter(), Map.of("status", status, "error", message));
    }
}
