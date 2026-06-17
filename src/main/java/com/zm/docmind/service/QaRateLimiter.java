package com.zm.docmind.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zm.docmind.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于用户的 QA 接口限流器，采用 Caffeine 固定窗口计数。
 * <p>每个用户在 1 分钟窗口内最多发起 {@code maxRequests} 次问答请求，超出抛 {@link RateLimitExceededException}（429），
 * 防止 LLM 成本滥用。
 * <p>并发安全：{@code Caffeine.get(key, loader)} 保证每个 key 只创建一个计数器，
 * {@code AtomicInteger.incrementAndGet} 保证计数的原子性。窗口边界处的轻微突刺对本场景可接受。
 */
@Component
public class QaRateLimiter {

    private final Cache<String, AtomicInteger> counters;
    private final int maxRequests;

    public QaRateLimiter(@Value("${docmind.qa.rate-limit-per-minute:10}") int maxRequests) {
        this.maxRequests = maxRequests;
        // expireAfterWrite（非 access）实现固定窗口：计数器自创建起 1 分钟后整块过期
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    /**
     * 尝试为指定用户消费一次配额，超限则抛 {@link RateLimitExceededException}。
     */
    public void checkAndConsume(String key) {
        AtomicInteger count = counters.get(key, k -> new AtomicInteger(0));
        if (count.incrementAndGet() > maxRequests) {
            throw new RateLimitExceededException("请求过于频繁，请稍后再试");
        }
    }
}
