package com.portfolio.tracker.security;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public String generateToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(86400); // 24 hours

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("portfolio-tracker")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(userId.toString())
                .claim("email", email)
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public UUID getUserIdFromToken(String token) {
        Jwt jwt = decoder.decode(token);
        return UUID.fromString(jwt.getSubject());
    }
}