package cl.duoc.ms_auth.controladores;

import cl.duoc.ms_auth.dtos.AuthLoginRequest;
import cl.duoc.ms_auth.dtos.AuthLoginResponse;
import cl.duoc.ms_auth.servicios.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestionar la autenticación de usuarios.
 * Proporciona endpoints para el inicio de sesión.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /**
     * Constructor para inyectar el servicio de autenticación.
     *
     * @param authService El servicio que maneja la lógica de autenticación.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint para el inicio de sesión de un usuario.
     * Valida las credenciales y, si son correctas, devuelve un token JWT.
     *
     * @param request El objeto {@link AuthLoginRequest} que contiene el nombre de usuario y la contraseña.
     * @return Un {@link ResponseEntity} con el {@link AuthLoginResponse} que incluye el token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@RequestBody @Valid AuthLoginRequest request) {
        logger.info("Iniciando proceso de login para el usuario: {}", request.username());
        AuthLoginResponse response = authService.login(request);
        logger.info("Login exitoso para el usuario: {}", request.username());
        return ResponseEntity.ok(response);
    }
}
