package cl.duoc.ms_auth.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Clase de configuración para la gestión de detalles de usuario en Spring Security.
 * Define cómo se codifican las contraseñas.
 */
@Configuration
public class CustomUserDetailsConfig {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsConfig.class);

    /**
     * Define el bean para el codificador de contraseñas.
     * Se utiliza BCryptPasswordEncoder, que es un estándar fuerte y seguro para el hashing de contraseñas.
     *
     * @return una instancia de {@link PasswordEncoder}.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.debug("Creando bean de PasswordEncoder (BCryptPasswordEncoder).");
        return new BCryptPasswordEncoder();
    }
}
