package cl.duoc.ms_auth.servicios.impl;

import cl.duoc.ms_auth.dtos.AuthLoginRequest;
import cl.duoc.ms_auth.dtos.AuthLoginResponse;
import cl.duoc.ms_auth.entidades.User;
import cl.duoc.ms_auth.exceptions.UnauthorizedException;
import cl.duoc.ms_auth.repositorio.UserRepository;
import cl.duoc.ms_auth.servicios.AuthService;
import cl.duoc.ms_auth.servicios.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Implementación del servicio de autenticación {@link AuthService}.
 * Maneja la lógica de negocio para el inicio de sesión de usuarios.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    /**
     * Constructor para inyectar las dependencias necesarias.
     *
     * @param userRepository  El repositorio para acceder a los datos de los usuarios.
     * @param passwordEncoder El codificador para verificar las contraseñas.
     * @param tokenService    El servicio para generar los tokens de acceso.
     */
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Procesa una solicitud de inicio de sesión.
     * Verifica las credenciales del usuario, su estado de activación y, si todo es correcto,
     * genera y devuelve un token de acceso junto con la información del usuario.
     *
     * @param request El DTO {@link AuthLoginRequest} que contiene el nombre de usuario y la contraseña.
     * @return Un DTO {@link AuthLoginResponse} con el token y los datos del usuario.
     * @throws UnauthorizedException si las credenciales son inválidas o el usuario está inactivo.
     */
    @Override
    public AuthLoginResponse login(AuthLoginRequest request) {
        logger.debug("Buscando usuario: {}", request.username());
        User u = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    logger.warn("Intento de login fallido para el usuario: {}. Razón: Usuario no encontrado.", request.username());
                    return new UnauthorizedException("Credenciales inválidas");
                });

        logger.debug("Verificando estado del usuario: {}", request.username());
        if (!"Y".equalsIgnoreCase(u.getActive())) {
            logger.warn("Intento de login fallido para el usuario: {}. Razón: Usuario inactivo.", request.username());
            throw new UnauthorizedException("Usuario inactivo");
        }

        logger.debug("Verificando contraseña para el usuario: {}", request.username());
        if (!passwordEncoder.matches(request.password(), u.getPasswordHash())) {
            logger.warn("Intento de login fallido para el usuario: {}. Razón: Contraseña incorrecta.", request.username());
            throw new UnauthorizedException("Credenciales inválidas");
        }

        logger.debug("Generando token para el usuario: {}", request.username());
        String token = tokenService.generateAccessToken(u);

        var roles = u.getRoles().stream().map(r -> r.getName()).sorted().collect(Collectors.toList());

        logger.info("Login exitoso para el usuario: {}", request.username());
        return new AuthLoginResponse(
                token,
                tokenService.getExpirationInstant(),
                u.getExternalId().toString(),
                u.getUsername(),
                roles,
                roles.contains("LAB_TECH") ? u.getLabCode() : null
        );
    }
}
