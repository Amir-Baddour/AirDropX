package org.example.Core.Airdrop.Model;

import java.math.BigDecimal;

public record RecipientInput(
        String address,
        BigDecimal amount
) {
}
