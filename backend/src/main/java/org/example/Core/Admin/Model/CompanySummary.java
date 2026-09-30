package org.example.Core.Admin.Model;

public record CompanySummary(
        String id,
        String name,
        String status,
        String ownerUsername,
        long members,
        long airdrops,
        long claims,
        String suspendedReason,
        String suspendedAt,
        String createdAt
) {
}
