package cl.duoc.ms_auth.servicios.impl;

import cl.duoc.ms_auth.entidades.Role;
import cl.duoc.ms_auth.entidades.User;
import cl.duoc.ms_auth.servicios.TokenService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de tokens {@link TokenService}.
 * Se encarga de la generación de tokens de acceso JWT utilizando claves asimétricas RSA.
 */
@Service
public class TokenServiceImpl implements TokenService {

    private final RSAKey rsaKey;
    private final String issuer;
    private final long expirationMinutes;
    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);

    /**
     * Constructor que inicializa el servicio de tokens.
     * Carga y valida las claves RSA pública y privada, el emisor y el tiempo de expiración desde las propiedades de la aplicación.
     *
     * @param privatePemRaw     La clave privada en formato PEM (PKCS#8), leída de las propiedades.
     * @param publicPemRaw      La clave pública en formato PEM, leída de las propiedades.
     * @param issuer            El emisor del token (issuer), leído de las propiedades.
     * @param expirationMinutes El tiempo de vida del token en minutos, leído de las propiedades.
     * @throws IllegalStateException si hay un error al cargar o validar las claves.
     */
    public TokenServiceImpl(
            @Value("${auth.jwt.rsa.private:}") String privatePemRaw,
            @Value("${auth.jwt.rsa.public:}")  String publicPemRaw,
            @Value("${auth.jwt.issuer}")        String issuer,
            @Value("${auth.jwt.expiration-minutes}") long expirationMinutes
    ) {
        try {
            logger.info("Inicializando TokenServiceImpl.");
            String privatePem = normalizePem(privatePemRaw);
            String publicPem  = normalizePem(publicPemRaw);

            require(!publicPem.isBlank(),  "auth.jwt.rsa.public vacío o no definido");
            require(!privatePem.isBlank(), "auth.jwt.rsa.private vacío o no definido");
            require(publicPem.contains("BEGIN PUBLIC KEY"),   "El PUBLIC PEM no tiene cabecera BEGIN PUBLIC KEY");
            require(privatePem.contains("BEGIN PRIVATE KEY"), "La PRIVATE PEM debe ser PKCS#8 (BEGIN PRIVATE KEY)");

            RSAKey parsed = RSAKey.parseFromPEMEncodedObjects(publicPem + "\n" + privatePem).toRSAKey();

            require(parsed.toRSAPublicKey() != null,  "No se pudo obtener la clave pública desde el PEM");
            require(parsed.toRSAPrivateKey() != null, "No se pudo obtener la clave privada (¿está en PKCS#8?)");

            this.rsaKey = parsed;
            this.issuer = issuer;
            this.expirationMinutes = expirationMinutes;
            logger.info("TokenServiceImpl inicializado correctamente.");

        } catch (Exception e) {
            logger.error("Error inicializando TokenServiceImpl: {}. Revisa auth.jwt.rsa.public / auth.jwt.rsa.private en application.properties (.env). La privada debe ser PKCS#8.", rootMsg(e), e);
            throw new IllegalStateException(
                    "Error inicializando TokenServiceImpl: " + rootMsg(e) +
                            ". Revisa auth.jwt.rsa.public / auth.jwt.rsa.private en application.properties (.env). " +
                            "La privada debe ser PKCS#8.", e);
        }
    }

    private static String normalizePem(String raw) {
        if (raw == null) return "";
        String clean = raw.replace("\"", "").replace(",", "");
        String newlineNormalized = clean.replace("\\n", "\n");
        return newlineNormalized.trim();
    }

    private static void require(boolean cond, String msg) {
        if (!cond) throw new IllegalArgumentException(msg);
    }

    private static String rootMsg(Throwable t) {
        Throwable x = t;
        while (x.getCause() != null) x = x.getCause();
        return x.getMessage();
    }


    @Override
    public String generateAccessToken(User user) {
        try {
            logger.debug("Generando token de acceso para el usuario: {}", user.getUsername());
            var now = Instant.now();
            var exp = now.plus(expirationMinutes, ChronoUnit.MINUTES);

            var roles = user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList());

            var claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(user.getUsername())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .claim("userId", user.getExternalId().toString())
                    .claim("roles", roles)
                    .claim("labCode", roles.contains("LAB_TECH") ? user.getLabCode() : null)
                    .build();

            var header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();

            var jwt = new SignedJWT(header, claims);

            jwt.sign(new RSASSASigner(rsaKey.toPrivateKey()));

            String token = jwt.serialize();
            logger.info("Token de acceso generado exitosamente para el usuario: {}", user.getUsername());
            return token;
        } catch (JOSEException e) {
            logger.error("Error generando JWT para el usuario: {}", user.getUsername(), e);
            throw new RuntimeException("Error generando JWT", e);
        }
    }

    @Override
    public Instant getExpirationInstant() {
        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }
}
