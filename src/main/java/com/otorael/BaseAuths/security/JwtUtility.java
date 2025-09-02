package com.otorael.BaseAuths.security;

import com.otorael.BaseAuths.model.Auths;
import io.jsonwebtoken.Claims;
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
import java.util.function.Function;

@Component
public class JwtUtility {

    @Value("${jwt.secret}")
    private String secretKey;

    private @NotNull SecretKey getSecretKey(){
        byte[] BYTE_KEY = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(BYTE_KEY);
    }

    public String generateToken(@NotNull Optional<Auths> auths){

        return Jwts.builder()
                .issuer("otorael.BaseAuths")
                .issuedAt(Date.from(Instant.now()))
                .signWith(getSecretKey())
                .subject(auths.get().getEmail())
                .expiration(Date.from(Instant.now().plusSeconds(3 * 60 * 60)))
                .notBefore(Date.from(Instant.now().minusSeconds(60)))
                .claim("jti", UUID.randomUUID().toString())
                .claim("firstName",auths.get().getFirstName())
                .claim("lastName",auths.get().getLastName())
                .claim("role",auths.get().getRole())
                .compact();
    }
    private Claims extractAllClaims(String token){
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    private  <T> T extractClaim(String token, @NotNull Function<Claims, T> claimsResolver){
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    public String getUserEmail(String token){
        return extractClaim(token, Claims::getSubject);
    }
}
