package org.example.Core.Airdrop.Model;

import java.math.BigDecimal;

public record Recipient(
        String id,
        String airdropId,
        String address,
        BigDecimal amount,
        RecipientStatus status,
        String txRef,
        String error,
        String updatedAt
) {
}
