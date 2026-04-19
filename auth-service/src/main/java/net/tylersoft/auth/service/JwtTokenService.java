package net.tylersoft.auth.service;

import lombok.RequiredArgsConstructor;
import net.tylersoft.auth.model.AuthCustomer;
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

    public long expiresInSeconds() {
        return expiryHours * 3600;
    }
}
