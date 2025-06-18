package com.otorael.BaseAuths.security;

import com.otorael.BaseAuths.model.Auths;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtUtility {

    @Value("${jwt.secret}")
    private String secretKey;
    public String generateToken(@NotNull Optional<Auths> auths){

        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .issuer("otorael.BaseAuths")
                .issuedAt(Date.from(Instant.now()))
                .signWith(key)
                .subject(auths.get().getEmail())
                .expiration(Date.from(Instant.now().plusSeconds(10 * 60)))
                .notBefore(Date.from(Instant.now().minusSeconds(60)))
                .claim("jti", UUID.randomUUID().toString())
                .claim("firstName",auths.get().getFirstName())
                .claim("lastName",auths.get().getLastName())
                .claim("role",auths.get().getRole())
                .compact();
    }
}
