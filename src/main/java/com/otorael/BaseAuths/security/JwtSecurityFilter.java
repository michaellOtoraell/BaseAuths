package com.otorael.BaseAuths.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtSecurityFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final Set<String> JWT_BLACKLIST_TOKEN = new HashSet<>();

    public static void blacklistToken(String token){
        JWT_BLACKLIST_TOKEN.add(token);
    }

    public boolean isTokenBlacklisted (String token){
        return JWT_BLACKLIST_TOKEN.contains(token);
    }
    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("authorization");
        if (authHeader != null && !authHeader.isEmpty()) {
            String token = authHeader.substring(7);
            if (isTokenBlacklisted(token)) {
                try {
                    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
                    Jws<Claims> jws = Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(token);
                    Claims claims = jws.getPayload();
                    String email = claims.getSubject();
                    String role = claims.get("role", String.class);

                    if (!email.isEmpty()){
                        if (role == null || role.isEmpty()){
                            response.sendError(HttpServletResponse.SC_NOT_FOUND,"Role is not found"); return;
                        }
                        UsernamePasswordAuthenticationToken authorization = new UsernamePasswordAuthenticationToken(
                                new User(email,"", Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+role))),
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+role))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authorization);
                    }
                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                    throw new RuntimeException(e);
                }
            }
        }
        filterChain.doFilter(request,response);
    }
}
