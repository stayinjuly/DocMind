package com.zm.docmind.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "this-is-a-very-long-secret-key-for-hmac-sha-256-algorithm-testing";
    private static final long EXPIRATION = 86400000L;
    private static final String USER_ID = "123";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("应生成非空 token")
        void shouldGenerateNonNullToken() {
            String token = jwtService.generateToken(USER_ID);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("不同 userId 应生成不同 token")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            String token1 = jwtService.generateToken("1");
            String token2 = jwtService.generateToken("2");
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("extractUserId")
    class ExtractUserId {

        @Test
        @DisplayName("应从合法 token 中正确提取 userId")
        void shouldExtractCorrectUserId() {
            String token = jwtService.generateToken(USER_ID);
            assertThat(jwtService.extractUserId(token)).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("合法 token 应返回包含 Claims 的 Optional")
        void validToken_returnsClaims() {
            String token = jwtService.generateToken(USER_ID);
            Optional<Claims> claims = jwtService.parse(token);
            assertThat(claims).isPresent();
            assertThat(claims.get().getSubject()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("合法 token 应返回 true")
        void validToken_returnsTrue() {
            String token = jwtService.generateToken(USER_ID);
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("过期 token 应返回 false")
        void expiredToken_returnsFalse() {
            JwtService expiredService = new JwtService(SECRET, 0L);
            String token = expiredService.generateToken(USER_ID);
            assertThat(jwtService.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("乱码字符串应返回 false")
        void malformedToken_returnsFalse() {
            assertThat(jwtService.isTokenValid("not.a.valid.token")).isFalse();
        }

        @Test
        @DisplayName("null token 应抛出异常（非 JwtException）")
        void nullToken_throwsException() {
            assertThatThrownBy(() -> jwtService.isTokenValid(null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("用不同密钥签名的 token 应返回 false")
        void wrongKeyToken_returnsFalse() {
            JwtService otherService = new JwtService(
                    "another-very-long-secret-key-for-hmac-sha-256-algorithm-testing", EXPIRATION);
            String token = otherService.generateToken(USER_ID);
            assertThat(jwtService.isTokenValid(token)).isFalse();
        }
    }
}
