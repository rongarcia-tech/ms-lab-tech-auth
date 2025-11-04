package cl.duoc.ms_lab.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class Json401EntryPoint implements AuthenticationEntryPoint {
    private static final ObjectMapper M = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(Json401EntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        logger.warn("Intento de acceso no autenticado desde {} a {}", request.getRemoteAddr(), request.getRequestURI());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        M.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", 401,
                "error", "Unauthorized",
                "message", authException != null ? authException.getMessage() : "Unauthorized",
                "path", request.getRequestURI()
        ));
    }
}
