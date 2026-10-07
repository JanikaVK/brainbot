package org.brainbot.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.brainbot.api.config.JwtProperties;
import org.brainbot.api.domain.AppUser;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtProperties properties;
    public JwtService(JwtProperties properties) { this.properties=properties; }
    private SecretKey key() {
        if (properties.secret()==null || properties.secret().getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT secret must contain at least 32 bytes");
        return Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }
    public String issue(AppUser user) {
        Instant now=Instant.now();
        return Jwts.builder().subject(user.getId().toString()).claim("role",user.getRole().name()).issuedAt(java.util.Date.from(now)).expiration(java.util.Date.from(now.plusSeconds(properties.expirationSeconds()))).signWith(key()).compact();
    }
    public io.jsonwebtoken.Claims parse(String token) { return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload(); }
}
