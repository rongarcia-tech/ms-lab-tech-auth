package cl.duoc.ms_lab.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.DefaultJWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resource Server “ligero”: valida JWT RS256 usando JWKS remoto del MS1 con cache.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtAuthProperties props;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtAuthProperties props) {
        try {
            this.props = props;

            // 1) HTTP retriever con timeouts y límite de tamaño
            ResourceRetriever retriever = new DefaultResourceRetriever(
                    2000,              // connect timeout ms
                    2000,              // read timeout ms
                    1024 * 1024        // 1MB
            );

            // 2) Cache del JWKSet (TTL 15 min, refresh a los 10 min)
            var jwkCache = new DefaultJWKSetCache();

            // 3) Fuente JWKS remota con cache
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(props.jwksUri()), retriever, jwkCache);

            // 4) Processor con selector de clave RS256 (elige por kid)
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);

            this.jwtProcessor = new DefaultJWTProcessor<>();
            this.jwtProcessor.setJWSKeySelector(keySelector);

        } catch (Exception e) {
            throw new IllegalStateException("No fue posible inicializar el validador JWT/JWKS", e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);

        try {
            UsernamePasswordAuthenticationToken auth = authenticate(token);
            if (auth != null) {
                auth.setDetails(req.getRemoteAddr());
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            // Token inválido → limpiamos contexto; el entry point responderá 401
            logger.warn("Error al validar el token JWT desde {}: {}", req.getRemoteAddr(), e.getMessage());
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        chain.doFilter(req, res);
    }

    private UsernamePasswordAuthenticationToken authenticate(String token) throws Exception {
        JWTClaimsSet claims = jwtProcessor.process(token, null);

        // ---- Validaciones de negocio/estándar ----
        if (!Objects.equals(props.issuer(), claims.getIssuer())) {
            throw new IllegalArgumentException("Issuer inválido");
        }

        long skew = props.allowedSkewSeconds() != null ? props.allowedSkewSeconds() : 60L;
        Instant now = Instant.now();

        Date exp = claims.getExpirationTime();
        if (exp == null || exp.toInstant().isBefore(now.minusSeconds(skew))) {
            throw new IllegalArgumentException("Token expirado");
        }

        Date iat = claims.getIssueTime();
        if (iat != null && iat.toInstant().isAfter(now.plusSeconds(skew))) {
            throw new IllegalArgumentException("Token con iat futuro");
        }

        String subject = claims.getSubject();
        String userId  = getStringClaim(claims, "userId");
        String labCode = getStringClaim(claims, "labCode");

        List<String> roles = getListStringClaim(claims, "roles");
        var authorities = roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        JwtUser principal = new JwtUser(subject, userId, Set.copyOf(roles), labCode);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private static String getStringClaim(JWTClaimsSet claims, String name) {
        Object v = claims.getClaim(name);
        return v != null ? String.valueOf(v) : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getListStringClaim(JWTClaimsSet claims, String name) {
        Object v = claims.getClaim(name);
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }
}
