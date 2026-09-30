package org.example.Core.Admin.Model;

import java.util.Map;

/** Platform-wide numbers for the admin overview. Maps are status -> count. */
public record PlatformStats(
        long users,
        Map<String, Long> companies,
        Map<String, Long> airdrops,
        Map<String, Long> claims,
        Map<String, Long> recipients,
        long claimsLast24h
) {
}
