package org.example.Core.Task.Verifier;

import org.example.Core.Task.Model.TaskType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TaskVerifierRegistry {
    private final Map<TaskType, TaskVerifier> verifiers = new EnumMap<>(TaskType.class);

    public TaskVerifierRegistry() {
        this(List.of(new QuizVerifier(), new SecretCodeVerifier(), new ManualProofVerifier()));
    }

    public TaskVerifierRegistry(List<TaskVerifier> list) {
        for (TaskVerifier v : list) {
            verifiers.put(v.type(), v);
        }
    }

    public TaskVerifier forType(TaskType type) {
        TaskVerifier verifier = verifiers.get(type);
        if (verifier == null) {
            throw new IllegalArgumentException("Unsupported task type: " + type);
        }
        return verifier;
    }
}
