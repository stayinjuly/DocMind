package com.zm.docmind.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "this-is-a-very-long-secret-key-for-hmac-sha-256-algorithm-testing";
    private static final long EXPIRATION = 86400000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("应生成非空且非空的 token")
        void shouldGenerateNonNullToken() {
            String token = jwtService.generateToken("user@test.com");
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("不同 email 应生成不同 token")
        void shouldGenerateDifferentTokensForDifferentEmails() {
            String token1 = jwtService.generateToken("a@test.com");
            String token2 = jwtService.generateToken("b@test.com");
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("extractEmail")
    class ExtractEmail {

        @Test
        @DisplayName("应从合法 token 中正确提取 email")
        void shouldExtractCorrectEmail() {
            String email = "user@test.com";
            String token = jwtService.generateToken(email);
            assertThat(jwtService.extractEmail(token)).isEqualTo(email);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("合法 token 应返回 true")
        void validToken_returnsTrue() {
            String token = jwtService.generateToken("user@test.com");
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("过期 token 应返回 false")
        void expiredToken_returnsFalse() {
            JwtService expiredService = new JwtService(SECRET, 0L);
            String token = expiredService.generateToken("user@test.com");
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
            String token = otherService.generateToken("user@test.com");
            assertThat(jwtService.isTokenValid(token)).isFalse();
        }
    }
}
