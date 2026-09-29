package org.example.Middleware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void blocksAfterLimitWithinWindow() {
        RateLimiter limiter = new RateLimiter(3, 60_000);
        assertTrue(limiter.tryAcquire("ip1", 0));
        assertTrue(limiter.tryAcquire("ip1", 10));
        assertTrue(limiter.tryAcquire("ip1", 20));
        assertFalse(limiter.tryAcquire("ip1", 30));
        assertTrue(limiter.tryAcquire("ip2", 30));
    }

    @Test
    void allowsAgainAfterWindowSlides() {
        RateLimiter limiter = new RateLimiter(2, 1_000);
        assertTrue(limiter.tryAcquire("k", 0));
        assertTrue(limiter.tryAcquire("k", 500));
        assertFalse(limiter.tryAcquire("k", 999));
        assertTrue(limiter.tryAcquire("k", 1_000));
    }

    @Test
    void cleanupDropsIdleKeys() {
        RateLimiter limiter = new RateLimiter(2, 1_000);
        limiter.tryAcquire("a", 0);
        limiter.tryAcquire("b", 5_000);
        limiter.cleanup(5_500);
        assertEquals(1, limiter.trackedKeys());
    }
}
