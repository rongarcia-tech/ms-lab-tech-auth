package cl.duoc.ms_auth.security;


import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private final KeyProvider keyProvider;
    private final String issuer;
    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    public WebSecurityConfig(KeyProvider keyProvider, @Value("${auth.jwt.issuer}") String issuer) {
        this.keyProvider = keyProvider;
        this.issuer = issuer;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.debug("Configurando la cadena de filtros de seguridad.");
        RSAKey rsaKey = keyProvider.rsaKey();
        var jwtUtils  = new JwtUtils(JwtUtils.toPublicKey(rsaKey), issuer);
        var jwtFilter = new JwtAuthFilter(jwtUtils);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    logger.debug("Configurando reglas de autorización HTTP.");
                    auth
                        .requestMatchers("/v3/api-docs/**","/swagger-ui.html","/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers("/roles/**").hasRole("ADMIN")
                        .requestMatchers("/users/me").authenticated()
                        .anyRequest().authenticated();
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        logger.debug("Cadena de filtros de seguridad configurada.");
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        logger.debug("Configurando CORS.");
        var cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept"));
        cfg.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/v3/api-docs/**", cfg);
        source.registerCorsConfiguration("/swagger-ui/**", cfg);
        source.registerCorsConfiguration("/**", cfg);
        logger.debug("CORS configurado.");
        return source;
    }

}
