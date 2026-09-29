package org.example.Core.Task.Verifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Model.TaskResult;
import org.example.Core.Task.Model.TaskType;

/**
 * Config:  {"questions": [{"question": "...", "options": ["a", "b", "c"], "answer": 1}]}
 * Answer:  [1, 0, 2]   (index of the chosen option, one per question)
 * Passes only if every answer is correct.
 */
public class QuizVerifier implements TaskVerifier {
    static final int MAX_QUESTIONS = 10;
    static final int MIN_OPTIONS = 2;
    static final int MAX_OPTIONS = 6;

    @Override
    public TaskType type() {
        return TaskType.QUIZ;
    }

    @Override
    public JsonObject prepareConfig(JsonObject config) {
        if (config == null || !config.has("questions") || !config.get("questions").isJsonArray()) {
            throw new IllegalArgumentException("Quiz config needs a 'questions' array");
        }
        JsonArray questions = config.getAsJsonArray("questions");
        if (questions.size() < 1 || questions.size() > MAX_QUESTIONS) {
            throw new IllegalArgumentException("A quiz needs 1 to " + MAX_QUESTIONS + " questions");
        }
        JsonArray clean = new JsonArray();
        for (int i = 0; i < questions.size(); i++) {
            String where = "Question " + (i + 1) + ": ";
            if (!questions.get(i).isJsonObject()) {
                throw new IllegalArgumentException(where + "must be an object");
            }
            JsonObject q = questions.get(i).getAsJsonObject();
            String text = text(q, "question");
            if (text == null || text.isBlank() || text.length() > 300) {
                throw new IllegalArgumentException(where + "'question' text is required (max 300 characters)");
            }
            if (!q.has("options") || !q.get("options").isJsonArray()) {
                throw new IllegalArgumentException(where + "'options' array is required");
            }
            JsonArray options = q.getAsJsonArray("options");
            if (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
                throw new IllegalArgumentException(where + "needs " + MIN_OPTIONS + " to " + MAX_OPTIONS + " options");
            }
            JsonArray cleanOptions = new JsonArray();
            for (JsonElement o : options) {
                if (!o.isJsonPrimitive() || o.getAsString().isBlank() || o.getAsString().length() > 200) {
                    throw new IllegalArgumentException(where + "every option must be non-empty text (max 200 characters)");
                }
                cleanOptions.add(o.getAsString().trim());
            }
            int answer = index(q.get("answer"));
            if (answer < 0 || answer >= options.size()) {
                throw new IllegalArgumentException(where + "'answer' must be the index of the correct option (0 to " + (options.size() - 1) + ")");
            }
            JsonObject cq = new JsonObject();
            cq.addProperty("question", text.trim());
            cq.add("options", cleanOptions);
            cq.addProperty("answer", answer);
            clean.add(cq);
        }
        JsonObject stored = new JsonObject();
        stored.add("questions", clean);
        return stored;
    }

    @Override
    public TaskResult verify(AirdropTask task, JsonElement answer) {
        JsonArray questions = task.config().getAsJsonArray("questions");
        if (answer == null || !answer.isJsonArray()) {
            return TaskResult.fail(task.id(), "Answer every question");
        }
        JsonArray given = answer.getAsJsonArray();
        if (given.size() != questions.size()) {
            return TaskResult.fail(task.id(), "Answer every question");
        }
        for (int i = 0; i < questions.size(); i++) {
            int expected = questions.get(i).getAsJsonObject().get("answer").getAsInt();
            if (index(given.get(i)) != expected) {
                // Don't reveal which question was wrong, to make guessing harder.
                return TaskResult.fail(task.id(), "Some answers are wrong");
            }
        }
        return TaskResult.pass(task.id());
    }

    @Override
    public JsonObject publicConfig(AirdropTask task) {
        JsonArray publicQuestions = new JsonArray();
        for (JsonElement e : task.config().getAsJsonArray("questions")) {
            JsonObject q = e.getAsJsonObject();
            JsonObject pq = new JsonObject();
            pq.addProperty("question", q.get("question").getAsString());
            pq.add("options", q.get("options"));
            publicQuestions.add(pq);
        }
        JsonObject result = new JsonObject();
        result.add("questions", publicQuestions);
        return result;
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    private static int index(JsonElement e) {
        try {
            if (e == null || !e.isJsonPrimitive()) {
                return -1;
            }
            return Integer.parseInt(e.getAsString().trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
