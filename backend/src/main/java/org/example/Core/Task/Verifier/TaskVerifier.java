package org.example.Core.Task.Verifier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Model.TaskResult;
import org.example.Core.Task.Model.TaskType;

/**
 * One implementation per task type (strategy pattern). Adding a new task type means adding
 * a new verifier and registering it in {@link TaskVerifierRegistry}; nothing else changes.
 */
public interface TaskVerifier {
    TaskType type();

    /**
     * Checks the config the company submitted and returns the config to store
     * (for example with secrets replaced by hashes).
     * @throws IllegalArgumentException if the config is invalid
     */
    JsonObject prepareConfig(JsonObject config);

    /** Checks one claimant's answer for this task. {@code answer} may be null if missing. */
    TaskResult verify(AirdropTask task, JsonElement answer);

    /** What claimants may see (never answers or secrets). */
    JsonObject publicConfig(AirdropTask task);
}
