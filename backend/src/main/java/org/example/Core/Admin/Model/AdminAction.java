package org.example.Core.Admin.Model;

/** One row of the admin audit log: who did what to which object, and why. */
public record AdminAction(
        long id,
        String adminId,
        String adminUsername,
        String action,
        String targetType,
        String targetId,
        String detail,
        String createdAt
) {
}
