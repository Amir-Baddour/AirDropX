package org.example.Core.Airdrop.Model;

import java.math.BigDecimal;
import java.util.Map;

public record AirdropStats(
        long totalRecipients,
        BigDecimal totalAmount,
        Map<String, Long> byStatus
) {
}
