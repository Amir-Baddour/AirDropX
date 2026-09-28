package org.example.Core.Payout;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockPayoutProviderTest {

    @Test
    void zeroFailureRateAlwaysSucceedsWithTxHash() {
        MockPayoutProvider provider = new MockPayoutProvider(0.0, new Random(42));
        for (int i = 0; i < 100; i++) {
            PayoutResult result = provider.send("0x1111111111111111111111111111111111111111", BigDecimal.ONE, "ABC");
            assertTrue(result.success());
            assertNotNull(result.txRef());
            assertEquals(66, result.txRef().length());
        }
    }

    @Test
    void fullFailureRateAlwaysFails() {
        MockPayoutProvider provider = new MockPayoutProvider(1.0, new Random(42));
        PayoutResult result = provider.send("0x1111111111111111111111111111111111111111", BigDecimal.ONE, "ABC");
        assertFalse(result.success());
        assertNotNull(result.error());
    }

    @Test
    void invalidFailureRateIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MockPayoutProvider(1.5));
        assertThrows(IllegalArgumentException.class, () -> new MockPayoutProvider(-0.1));
    }
}
