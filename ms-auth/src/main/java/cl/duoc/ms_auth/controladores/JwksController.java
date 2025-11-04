package cl.duoc.ms_auth.controladores;

import cl.duoc.ms_auth.security.KeyProvider;
import com.nimbusds.jose.jwk.JWKSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para exponer el endpoint de JWKS (JSON Web Key Set).
 * Este endpoint es utilizado por los clientes para obtener la clave pública
 * necesaria para verificar la firma de los tokens JWT emitidos por este servicio.
 */
@RestController
@RequestMapping("/.well-known")
public class JwksController {
    private final KeyProvider keyProvider;
    private static final Logger logger = LoggerFactory.getLogger(JwksController.class);

    /**
     * Constructor que inyecta el proveedor de claves.
     *
     * @param keyProvider El proveedor utilizado para obtener las claves JWK.
     */
    public JwksController(KeyProvider keyProvider){ this.keyProvider = keyProvider; }

    /**
     * Devuelve el JSON Web Key Set (JWKS) público.
     * Este conjunto de claves contiene la clave pública que los clientes pueden usar
     * para verificar la firma de los tokens JWT.
     *
     * @return Un {@link ResponseEntity} con el JWKSet en formato JSON.
     */
    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> jwks() {
        logger.info("Solicitud recibida para obtener el JWKS.");
        var jwkSet = new JWKSet(keyProvider.publicJwk());
        logger.debug("JWKS generado exitosamente.");
        return ResponseEntity.ok(jwkSet.toString(true));
    }
}
