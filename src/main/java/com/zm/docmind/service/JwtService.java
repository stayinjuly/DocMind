package com.zm.docmind.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 令牌服务
 * 负责令牌的生成、解析和验证。
 * <p>subject 为用户标识（{@code sys_user.id} 的字符串形式），而非 email：
 * 避免在 payload 中暴露登录邮箱，并保证用户改邮箱后标识仍稳定。
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT 令牌
     *
     * @param userId 用户标识（sys_user.id 的字符串形式）
     */
    public String generateToken(String userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    /**
     * 从令牌中提取用户标识
     */
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 验证令牌是否有效
     */
    public boolean isTokenValid(String token) {
        return parse(token).isPresent();
    }

    /**
     * 安全解析令牌，失败返回 {@link Optional#empty()}。
     * 供过滤器单次解析后复用 Claims，避免在 isTokenValid + extractUserId 中重复解析。
     */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(parseClaims(token));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
