package com.ll.Yuruppang.global.security;

import com.ll.Yuruppang.domain.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
//    private final long ACCESS_TOKEN_EXP = 1000L * 10; // 테스트용 10초
    private final long REFRESH_TOKEN_EXP = 1000L * 60 * 60 * 24 * 7; // 7일

    public String createAccessToken(User user) {
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + ACCESS_TOKEN_EXP);

        Map<String, ? extends Serializable> body = Map.of("id", user.getId(), "username", user.getUsername());

        return Jwts.builder()
                .setClaims(body)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(User user) {
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

        Map<String, ? extends Serializable> body = Map.of("id", user.getId(), "username", user.getUsername());

        return Jwts.builder()
                .setClaims(body)
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXP))
                .signWith(secretKey)
                .compact();
    }

    public Map<String, Object> parse(String token) {
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

            return Jwts
                    .parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (ExpiredJwtException e) {
            // 토큰은 유효했지만 만료된 경우
            return Map.of();
        } catch (JwtException e) {
            // 위조된 토큰, 잘못된 서명, 형식 오류 등
            throw new IllegalArgumentException("잘못된 토큰입니다.", e);
        }
    }
}