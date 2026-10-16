package app.crewup;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * A stand-in for Keycloak in backend tests. It serves the two endpoints the backend reads through {@code issuer-uri}
 * (OIDC discovery and the public keys), so tests run the same token decoder as production: signature, issuer, expiry
 * and audience checks included. The key pair is generated per test run, so no private key is committed.
 */
public final class TestOidcProvider {

    public static final String API_AUDIENCE = "crewup-api";
    public static final String SUBJECT = "test-user-subject";

    private static final String REALM_PATH = "/realms/crewup";
    private static final String KEY_ID = "test-key";
    private static final TestOidcProvider INSTANCE = start();

    private final String issuer;
    private final JwtEncoder encoder;

    private TestOidcProvider(String issuer, JwtEncoder encoder) {
        this.issuer = issuer;
        this.encoder = encoder;
    }

    public static TestOidcProvider instance() {
        return INSTANCE;
    }

    public String issuer() {
        return issuer;
    }

    /** A token the backend should accept. */
    public String validToken() {
        return token(API_AUDIENCE);
    }

    /** Identical to {@link #validToken()} except for the audience, so a rejection can only come from the audience. */
    public String token(String audience) {
        return sign(audience, Map.of());
    }

    /** A token the backend should accept, carrying profile claims such as {@code name} and {@code email}. */
    public String validTokenWithClaims(Map<String, Object> profileClaims) {
        return sign(API_AUDIENCE, profileClaims);
    }

    private String sign(String audience, Map<String, Object> profileClaims) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(SUBJECT)
                .audience(List.of(audience))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(5)))
                .claims(all -> all.putAll(profileClaims))
                .build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY_ID).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static TestOidcProvider start() {
        try {
            RSAKey key = generateKey();
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            String issuer = "http://127.0.0.1:" + server.getAddress().getPort() + REALM_PATH;
            String jwksUri = issuer + "/protocol/openid-connect/certs";

            // Same paths as Keycloak, so the issuer URL is the only thing tests change.
            String discovery = JSONObjectUtils.toJSONString(Map.of("issuer", issuer, "jwks_uri", jwksUri));
            String publicKeys = new JWKSet(key.toPublicJWK()).toString();
            server.createContext(
                    REALM_PATH + "/.well-known/openid-configuration", exchange -> respond(exchange, discovery));
            server.createContext(
                    REALM_PATH + "/protocol/openid-connect/certs", exchange -> respond(exchange, publicKeys));
            server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "test-oidc-provider");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();

            return new TestOidcProvider(issuer, new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key))));
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Could not start the test OIDC provider", e);
        }
    }

    private static RSAKey generateKey() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID(KEY_ID)
                .build();
    }

    private static void respond(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
