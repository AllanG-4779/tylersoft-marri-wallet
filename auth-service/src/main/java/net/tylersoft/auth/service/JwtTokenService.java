package net.tylersoft.auth.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.model.AuthAdmin;
import net.tylersoft.auth.model.AuthCustomer;
import net.tylersoft.auth.model.AuthIntegrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder encoder;

    @Value("${jwt.expiry-hours:24}")
    private long expiryHours;

    public String issueCustomerToken(AuthCustomer customer, String deviceId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mari-wallet")
                .subject(customer.getId().toString())
                .claim("phone", customer.getPhoneNumber())
                .claim("role", "CUSTOMER")
                .claim("deviceId", deviceId)
                .issuedAt(now)
                .expiresAt(now.plus(expiryHours, ChronoUnit.HOURS))
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String issueAdminToken(AuthAdmin admin) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mari-wallet")
                .subject(admin.getId().toString())
                .claim("username", admin.getUsername())
                .claim("role", "SYSTEM_ADMIN")
                .issuedAt(now)
                .expiresAt(now.plus(expiryHours, ChronoUnit.HOURS))
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String issueIntegratorToken(AuthIntegrator integrator) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mari-wallet")
                .subject(integrator.getId().toString())
                .claim("name", integrator.getName())
                .claim("accessKey", integrator.getAccessKey())
                .claim("role", "INTEGRATOR")
                .issuedAt(now)
                .expiresAt(now.plus(expiryHours, ChronoUnit.HOURS))
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return expiryHours * 3600;
    }
}
