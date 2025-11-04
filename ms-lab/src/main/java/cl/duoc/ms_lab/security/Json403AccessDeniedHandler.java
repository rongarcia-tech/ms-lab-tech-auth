package cl.duoc.ms_lab.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class Json403AccessDeniedHandler implements AccessDeniedHandler {
    private static final ObjectMapper M = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(Json403AccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException accessDeniedException) throws IOException {
        logger.warn("Acceso denegado para el usuario '{}' desde {} a {}", request.getRemoteUser(), request.getRemoteAddr(), request.getRequestURI());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        M.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", 403,
                "error", "Forbidden",
                "message", accessDeniedException != null ? accessDeniedException.getMessage() : "Forbidden",
                "path", request.getRequestURI()
        ));
    }
}
