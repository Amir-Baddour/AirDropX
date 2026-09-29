package org.example.Core.Task.Verifier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Model.TaskResult;
import org.example.Core.Task.Model.TaskType;

/**
 * For tasks that can't be checked automatically (for example a social media follow).
 * Config:  {"instructions": "Follow @acme on X and paste your profile link"}
 * Answer:  "https://x.com/alice"   (1-500 characters), reviewed by the company.
 */
public class ManualProofVerifier implements TaskVerifier {
    static final int MAX_PROOF_LENGTH = 500;

    @Override
    public TaskType type() {
        return TaskType.MANUAL_PROOF;
    }

    @Override
    public JsonObject prepareConfig(JsonObject config) {
        JsonObject stored = new JsonObject();
        String instructions = config != null && config.has("instructions") && config.get("instructions").isJsonPrimitive()
                ? config.get("instructions").getAsString().trim() : "";
        if (instructions.isEmpty() || instructions.length() > 500) {
            throw new IllegalArgumentException("Manual proof config needs 'instructions' (max 500 characters)");
        }
        stored.addProperty("instructions", instructions);
        return stored;
    }

    @Override
    public TaskResult verify(AirdropTask task, JsonElement answer) {
        if (answer == null || !answer.isJsonPrimitive() || answer.getAsString().isBlank()) {
            return TaskResult.fail(task.id(), "Provide your proof");
        }
        String proof = answer.getAsString().trim();
        if (proof.length() > MAX_PROOF_LENGTH) {
            return TaskResult.fail(task.id(), "Proof is too long (max " + MAX_PROOF_LENGTH + " characters)");
        }
        return TaskResult.review(task.id(), proof);
    }

    @Override
    public JsonObject publicConfig(AirdropTask task) {
        JsonObject result = new JsonObject();
        result.addProperty("instructions", task.config().get("instructions").getAsString());
        return result;
    }
}
