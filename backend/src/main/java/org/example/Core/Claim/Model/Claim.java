package org.example.Core.Claim.Model;

import org.example.Core.Task.Model.TaskResult;

import java.util.List;

public record Claim(
        String id,
        String airdropId,
        String address,
        ClaimStatus status,
        String rejectReason,
        String createdAt,
        String reviewedAt,
        List<TaskResult> results
) {
}
