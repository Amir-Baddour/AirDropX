package org.example.Core.Task;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Model.TaskResult;
import org.example.Core.Task.Model.TaskType;
import org.example.Core.Task.Verifier.ManualProofVerifier;
import org.example.Core.Task.Verifier.QuizVerifier;
import org.example.Core.Task.Verifier.SecretCodeVerifier;
import org.example.Core.Task.Verifier.TaskVerifier;
import org.example.Core.Task.Verifier.TaskVerifierRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskVerifierTest {
    private static final String TASK_ID = "00000000-0000-0000-0000-000000000001";

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static AirdropTask task(TaskVerifier verifier, String config) {
        return new AirdropTask(TASK_ID, "a", verifier.type(), "t", null, verifier.prepareConfig(json(config)), 1);
    }

    @Test
    void quizPassesOnlyWithAllCorrectAnswers() {
        QuizVerifier v = new QuizVerifier();
        AirdropTask t = task(v, "{\"questions\":[{\"question\":\"Q1\",\"options\":[\"a\",\"b\"],\"answer\":1},"
                + "{\"question\":\"Q2\",\"options\":[\"x\",\"y\",\"z\"],\"answer\":2}]}");
        assertTrue(v.verify(t, JsonParser.parseString("[1,2]")).passed());
        assertTrue(v.verify(t, JsonParser.parseString("[\"1\",\"2\"]")).passed());
        assertFalse(v.verify(t, JsonParser.parseString("[1,0]")).passed());
        assertFalse(v.verify(t, JsonParser.parseString("[1]")).passed());
        assertFalse(v.verify(t, null).passed());
        assertFalse(v.verify(t, new JsonPrimitive("1")).passed());
    }

    @Test
    void quizPublicConfigHidesAnswers() {
        QuizVerifier v = new QuizVerifier();
        AirdropTask t = task(v, "{\"questions\":[{\"question\":\"Q1\",\"options\":[\"a\",\"b\"],\"answer\":1}]}");
        JsonObject pub = v.publicConfig(t);
        JsonObject q = pub.getAsJsonArray("questions").get(0).getAsJsonObject();
        assertFalse(q.has("answer"));
        assertEquals(2, q.getAsJsonArray("options").size());
    }

    @Test
    void quizRejectsBadConfig() {
        QuizVerifier v = new QuizVerifier();
        assertThrows(IllegalArgumentException.class, () -> v.prepareConfig(json("{}")));
        assertThrows(IllegalArgumentException.class, () -> v.prepareConfig(json("{\"questions\":[]}")));
        assertThrows(IllegalArgumentException.class, () -> v.prepareConfig(json(
                "{\"questions\":[{\"question\":\"Q\",\"options\":[\"only one\"],\"answer\":0}]}")));
        assertThrows(IllegalArgumentException.class, () -> v.prepareConfig(json(
                "{\"questions\":[{\"question\":\"Q\",\"options\":[\"a\",\"b\"],\"answer\":5}]}")));
    }

    @Test
    void secretCodeIsHashedAndCaseInsensitive() {
        SecretCodeVerifier v = new SecretCodeVerifier();
        AirdropTask t = task(v, "{\"code\":\"Launch2026\",\"hint\":\"end of the video\"}");
        assertFalse(t.config().toString().contains("Launch2026"));
        assertFalse(t.config().toString().toLowerCase().contains("launch2026"));
        assertTrue(v.verify(t, new JsonPrimitive("  launch2026 ")).passed());
        assertFalse(v.verify(t, new JsonPrimitive("launch2025")).passed());
        assertFalse(v.verify(t, null).passed());
        JsonObject pub = v.publicConfig(t);
        assertEquals("end of the video", pub.get("hint").getAsString());
        assertFalse(pub.has("code_hash"));
    }

    @Test
    void secretCodeRejectsShortCode() {
        SecretCodeVerifier v = new SecretCodeVerifier();
        assertThrows(IllegalArgumentException.class, () -> v.prepareConfig(json("{\"code\":\"ab\"}")));
        assertThrows(IllegalArgumentException.class, () -> v.prepareConfig(json("{}")));
    }

    @Test
    void manualProofGoesToReview() {
        ManualProofVerifier v = new ManualProofVerifier();
        AirdropTask t = task(v, "{\"instructions\":\"Paste the link to your tweet\"}");
        TaskResult r = v.verify(t, new JsonPrimitive("https://x.com/me/status/1"));
        assertTrue(r.passed());
        assertTrue(r.needsReview());
        assertEquals("https://x.com/me/status/1", r.proof());
        assertFalse(v.verify(t, new JsonPrimitive("   ")).passed());
        assertFalse(v.verify(t, null).passed());
    }

    @Test
    void registryHasAVerifierForEveryType() {
        TaskVerifierRegistry registry = new TaskVerifierRegistry();
        for (TaskType type : TaskType.values()) {
            assertEquals(type, registry.forType(type).type());
        }
        assertTrue(TaskType.QUIZ.isAutoVerified());
        assertFalse(TaskType.MANUAL_PROOF.isAutoVerified());
    }

    @Test
    void nonObjectAnswersNeverCrash() {
        JsonElement[] weird = {null, new JsonArray(), new JsonObject(), new JsonPrimitive(42)};
        TaskVerifier[] verifiers = {new QuizVerifier(), new SecretCodeVerifier(), new ManualProofVerifier()};
        String[] configs = {"{\"questions\":[{\"question\":\"Q\",\"options\":[\"a\",\"b\"],\"answer\":0}]}",
                "{\"code\":\"abcd\"}", "{\"instructions\":\"x\"}"};
        for (int i = 0; i < verifiers.length; i++) {
            AirdropTask t = task(verifiers[i], configs[i]);
            for (JsonElement answer : weird) {
                verifiers[i].verify(t, answer);
            }
        }
    }
}
