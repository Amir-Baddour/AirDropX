package org.example.Core.Task.Verifier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Model.TaskResult;
import org.example.Core.Task.Model.TaskType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Config:  {"code": "LAUNCH2026", "hint": "Watch our launch video until the end"}
 * Stored:  {"code_hash": "<sha-256>", "hint": "..."}   (the plain code is never saved)
 * Answer:  "launch2026"   (case and surrounding spaces are ignored)
 */
public class SecretCodeVerifier implements TaskVerifier {

    @Override
    public TaskType type() {
        return TaskType.SECRET_CODE;
    }

    @Override
    public JsonObject prepareConfig(JsonObject config) {
        if (config == null || !config.has("code") || !config.get("code").isJsonPrimitive()) {
            throw new IllegalArgumentException("Secret code config needs a 'code'");
        }
        String code = normalize(config.get("code").getAsString());
        if (code.length() < 4 || code.length() > 64) {
            throw new IllegalArgumentException("Secret code must be 4 to 64 characters");
        }
        JsonObject stored = new JsonObject();
        stored.addProperty("code_hash", sha256(code));
        if (config.has("hint") && config.get("hint").isJsonPrimitive()) {
            String hint = config.get("hint").getAsString().trim();
            if (hint.length() > 300) {
                throw new IllegalArgumentException("Hint is too long (max 300 characters)");
            }
            stored.addProperty("hint", hint);
        }
        return stored;
    }

    @Override
    public TaskResult verify(AirdropTask task, JsonElement answer) {
        if (answer == null || !answer.isJsonPrimitive() || answer.getAsString().isBlank()) {
            return TaskResult.fail(task.id(), "Enter the secret code");
        }
        byte[] expected = task.config().get("code_hash").getAsString().getBytes(StandardCharsets.UTF_8);
        byte[] actual = sha256(normalize(answer.getAsString())).getBytes(StandardCharsets.UTF_8);
        // Constant-time comparison, so response time doesn't leak how close a guess was.
        if (MessageDigest.isEqual(expected, actual)) {
            return TaskResult.pass(task.id());
        }
        return TaskResult.fail(task.id(), "The secret code is not correct");
    }

    @Override
    public JsonObject publicConfig(AirdropTask task) {
        JsonObject result = new JsonObject();
        if (task.config().has("hint")) {
            result.addProperty("hint", task.config().get("hint").getAsString());
        }
        return result;
    }

    static String normalize(String code) {
        return code.trim().toLowerCase(Locale.ROOT);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
