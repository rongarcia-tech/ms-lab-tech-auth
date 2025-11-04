package cl.duoc.ms_auth.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

public class JwtUtils {
    private final RSAPublicKey publicKey;
    private final String expectedIssuer;
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    public JwtUtils(RSAPublicKey publicKey, String expectedIssuer) {
        this.publicKey = publicKey;
        this.expectedIssuer = expectedIssuer;
    }

    public Optional<JwtPayload> validateAndParse(String token) {
        try {
            logger.debug("Iniciando validación de token JWT.");
            SignedJWT jwt = SignedJWT.parse(token);
            logger.debug("Token parseado correctamente.");

            var header = jwt.getHeader();
            if (!Objects.equals(header.getType(), JOSEObjectType.JWT)) {
                logger.debug("Validación fallida: El tipo de cabecera no es JWT.");
                return Optional.empty();
            }
            logger.debug("Tipo de cabecera verificado.");

            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!jwt.verify(verifier)) {
                logger.debug("Validación fallida: La firma del token es inválida.");
                return Optional.empty();
            }
            logger.debug("Firma del token verificada.");

            var claims = jwt.getJWTClaimsSet();

            if (expectedIssuer != null && !expectedIssuer.equals(claims.getIssuer())) {
                logger.debug("Validación fallida: El emisor del token no coincide. Esperado: {}, Recibido: {}", expectedIssuer, claims.getIssuer());
                return Optional.empty();
            }
            logger.debug("Emisor del token verificado.");

            var now = Instant.now();
            if (claims.getExpirationTime() == null || now.isAfter(claims.getExpirationTime().toInstant())) {
                logger.debug("Validación fallida: El token ha expirado o no tiene fecha de expiración.");
                return Optional.empty();
            }
            logger.debug("Fecha de expiración verificada.");

            String subject = claims.getSubject();
            String userId  = Objects.toString(claims.getClaim("userId"), null);
            String labCode = Objects.toString(claims.getClaim("labCode"), null);

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getClaim("roles");
            if (roles == null) roles = List.of();
            
            logger.debug("Payload extraído exitosamente para el usuario: {}", subject);

            return Optional.of(new JwtPayload(subject, userId, roles, labCode));
        } catch (Exception e) {
            logger.debug("Error durante la validación del token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static RSAPublicKey toPublicKey(RSAKey rsaKey) {
        try { return rsaKey.toRSAPublicKey(); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public record JwtPayload(String username, String userId, List<String> roles, String labCode) {}
}
