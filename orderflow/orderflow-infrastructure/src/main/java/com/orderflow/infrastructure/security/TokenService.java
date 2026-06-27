package com.orderflow.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Issues short-lived access tokens for authenticated users. */
@Service
public class TokenService {

    private static final long TTL_MINUTES = 60;

    private final JwtEncoder encoder;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    public String issue(UserDetails user) {
        Instant now = Instant.now(clock);
        List<String> roles = user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
            .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("orderflow")
            .issuedAt(now)
            .expiresAt(now.plus(TTL_MINUTES, ChronoUnit.MINUTES))
            .subject(user.getUsername())
            .claim("roles", roles)
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
