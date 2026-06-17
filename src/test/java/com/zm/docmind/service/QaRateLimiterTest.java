package com.zm.docmind.service;

import com.zm.docmind.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QaRateLimiterTest {

    private QaRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new QaRateLimiter(3);
    }

    @Test
    @DisplayName("未超限的请求应全部通过")
    void withinLimit_passes() {
        for (int i = 0; i < 3; i++) {
            assertThatCode(() -> rateLimiter.checkAndConsume("user@test.com"))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("超过配额应抛 RateLimitExceededException")
    void overLimit_throws() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.checkAndConsume("user@test.com");
        }
        assertThatThrownBy(() -> rateLimiter.checkAndConsume("user@test.com"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("不同用户的配额相互独立")
    void differentUsers_independent() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.checkAndConsume("a@test.com");
        }
        // a 已达上限，b 仍可请求
        assertThatCode(() -> rateLimiter.checkAndConsume("b@test.com"))
                .doesNotThrowAnyException();
    }
}
