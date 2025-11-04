package cl.duoc.ms_auth.security;

import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KeyProvider {
    private final RSAKey rsaKey;
    private static final Logger logger = LoggerFactory.getLogger(KeyProvider.class);

    public KeyProvider(
            @Value("${auth.jwt.rsa.public}") String publicPem,
            @Value("${auth.jwt.rsa.private}") String privatePem) throws Exception {
        logger.debug("Cargando claves RSA desde las propiedades.");
        try {
            this.rsaKey = RSAKey.parseFromPEMEncodedObjects(publicPem + "\n" + privatePem).toRSAKey();
            logger.debug("Claves RSA cargadas y parseadas exitosamente.");
        } catch (Exception e) {
            logger.error("Error al parsear las claves RSA: {}", e.getMessage());
            throw e;
        }
    }

    public RSAKey rsaKey() {
        logger.debug("Proveyendo clave RSA completa.");
        return rsaKey;
    }

    public RSAKey publicJwk() {
        logger.debug("Proveyendo clave pública JWK.");
        return rsaKey.toPublicJWK();
    }
}
