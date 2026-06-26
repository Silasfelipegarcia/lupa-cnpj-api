package br.com.lupainsights.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpRateLimiterTest {

    private final IpRateLimiter limiter = new IpRateLimiter(new RedisRateLimiter(new br.com.lupainsights.config.SecurityProperties()));

    @Test
    void deveBloquearAposExcederLimite() {
        String key = "127.0.0.1:POST:/cnpj/import";

        assertTrue(limiter.tryAcquire(key, 2, 60_000));
        assertTrue(limiter.tryAcquire(key, 2, 60_000));
        assertFalse(limiter.tryAcquire(key, 2, 60_000));
    }
}
