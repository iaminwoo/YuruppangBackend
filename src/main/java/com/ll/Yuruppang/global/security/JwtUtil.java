package com.ll.Yuruppang.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private final long ACCESS_TOKEN_EXP = 1000L * 60 * 15; // 15분
    private final long REFRESH_TOKEN_EXP = 1000L * 60 * 60 * 24 * 7; // 7일

    public String createAccessToken(Long userId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + ACCESS_TOKEN_EXP);

        Map<String, ? extends Serializable> body = Map.of("id", userId);

        return Jwts.builder()
                .setClaims(body)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

        Map<String, ? extends Serializable> body = Map.of("id", userId);

        return Jwts.builder()
                .setClaims(body)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXP))
                .signWith(secretKey)
                .compact();
    }

    public Map<String, Object> parse(String token) {
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts
                .parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}