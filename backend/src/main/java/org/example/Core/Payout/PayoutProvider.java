package org.example.Core.Payout;

import java.math.BigDecimal;

/**
 * Sends one payout. The platform is Web2 with mocked payouts today;
 * a real chain integration only needs a new implementation of this interface.
 */
public interface PayoutProvider {
    PayoutResult send(String address, BigDecimal amount, String tokenSymbol);
}
