package org.example.Core.Payout;

import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.Random;

/**
 * Simulated payout: succeeds or fails at a configurable rate and returns a fake transaction hash.
 */
public class MockPayoutProvider implements PayoutProvider {
    private final double failureRate;
    private final Random random;

    public MockPayoutProvider(double failureRate) {
        this(failureRate, new Random());
    }

    public MockPayoutProvider(double failureRate, Random random) {
        if (failureRate < 0 || failureRate > 1) {
            throw new IllegalArgumentException("failureRate must be between 0 and 1");
        }
        this.failureRate = failureRate;
        this.random = random;
    }

    @Override
    public PayoutResult send(String address, BigDecimal amount, String tokenSymbol) {
        if (random.nextDouble() < failureRate) {
            return PayoutResult.failed("Simulated network error");
        }
        byte[] hash = new byte[32];
        random.nextBytes(hash);
        return PayoutResult.ok("0x" + HexFormat.of().formatHex(hash));
    }
}
