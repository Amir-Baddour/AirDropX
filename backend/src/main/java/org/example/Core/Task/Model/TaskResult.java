package org.example.Core.Task.Model;

/**
 * Outcome of checking one task for one claim.
 * @param passed      true if the task is satisfied (for manual tasks: proof was provided)
 * @param needsReview true if a person must still review the proof
 * @param proof       what the claimant submitted (kept for review), may be null
 * @param detail      short explanation, shown to the claimant on failure
 */
public record TaskResult(
        String taskId,
        boolean passed,
        boolean needsReview,
        String proof,
        String detail
) {
    public static TaskResult pass(String taskId) {
        return new TaskResult(taskId, true, false, null, null);
    }

    public static TaskResult fail(String taskId, String detail) {
        return new TaskResult(taskId, false, false, null, detail);
    }

    public static TaskResult review(String taskId, String proof) {
        return new TaskResult(taskId, true, true, proof, "Waiting for review");
    }
}
