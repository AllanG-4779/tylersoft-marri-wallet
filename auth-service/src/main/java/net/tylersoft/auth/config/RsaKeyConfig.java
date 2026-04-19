package net.tylersoft.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Configuration
public class RsaKeyConfig {

    @Value("${jwt.private-key-path:#{null}}")
    private Resource privateKeyResource;

    @Value("${jwt.public-key-path:#{null}}")
    private Resource publicKeyResource;

    @Bean
    public RSAKey rsaKey() throws Exception {
        if (privateKeyResource == null || publicKeyResource == null) {
            log.warn("=======================================================");
            log.warn("JWT_PRIVATE_KEY_PATH / JWT_PUBLIC_KEY_PATH not set.");
            log.warn("Generating an EPHEMERAL RSA key pair — NOT FOR PRODUCTION.");
            log.warn("All tokens will be invalidated on service restart.");
            log.warn("=======================================================");
            return generateEphemeral();
        }
        RSAPrivateKey privateKey = parsePrivateKey(new String(privateKeyResource.getInputStream().readAllBytes()));
        RSAPublicKey publicKey = parsePublicKey(new String(publicKeyResource.getInputStream().readAllBytes()));
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("mari-wallet-key")
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    @Bean
    public JWKSet jwkSet(RSAKey rsaKey) {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    private RSAKey generateEphemeral() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID("mari-wallet-key")
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(stripped);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private RSAPublicKey parsePublicKey(String pem) throws Exception {
        String stripped = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(stripped);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }
}
