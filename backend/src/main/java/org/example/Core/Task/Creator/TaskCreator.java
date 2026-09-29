package org.example.Core.Task.Creator;

import com.google.gson.JsonObject;
import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Airdrop.Exception.InvalidAirdropStateException;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Airdrop.Model.AirdropStatus;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Model.TaskType;
import org.example.Core.Task.Verifier.TaskVerifierRegistry;
import org.example.Infra.Persistence.Airdrop.AirdropEventRepository;
import org.example.Infra.Persistence.Airdrop.AirdropRepository;
import org.example.Infra.Persistence.Task.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskCreator {
    private static final Logger logger = LoggerFactory.getLogger(TaskCreator.class.getName());
    public static final int MAX_TASKS_PER_AIRDROP = 10;
    private final AirdropRepository airdropRepository;
    private final TaskRepository taskRepository;
    private final AirdropEventRepository eventRepository;
    private final TaskVerifierRegistry registry;

    public TaskCreator() {
        this.airdropRepository = new AirdropRepository();
        this.taskRepository = new TaskRepository();
        this.eventRepository = new AirdropEventRepository();
        this.registry = new TaskVerifierRegistry();
    }

    /** Adds a task to a DRAFT airdrop whose claims are closed. The config is checked by the task type's verifier. */
    public AirdropTask createTask(String companyId, String airdropId, String typeText, String title,
                                  String description, JsonObject config) throws Exception {
        Airdrop airdrop = editableAirdrop(companyId, airdropId);
        TaskType type = parseType(typeText);
        if (title == null || title.isBlank() || title.trim().length() > 150) {
            throw new IllegalArgumentException("Task title is required (max 150 characters)");
        }
        if (description != null && description.length() > 1000) {
            throw new IllegalArgumentException("Task description is too long (max 1000 characters)");
        }
        if (taskRepository.listByAirdrop(airdropId).size() >= MAX_TASKS_PER_AIRDROP) {
            throw new IllegalArgumentException("An airdrop can have at most " + MAX_TASKS_PER_AIRDROP + " tasks");
        }
        JsonObject stored = registry.forType(type).prepareConfig(config == null ? new JsonObject() : config);
        AirdropTask task = taskRepository.create(airdrop.id(), type, title.trim(), description, stored);
        eventRepository.add(airdropId, "TASK_ADDED", type + " task added: " + task.title());
        logger.info("Task {} ({}) added to airdrop {}", task.id(), type, airdropId);
        return task;
    }

    public void deleteTask(String companyId, String airdropId, String taskId) throws Exception {
        editableAirdrop(companyId, airdropId);
        if (!taskRepository.delete(airdropId, taskId)) {
            throw new AirdropNotFoundException("Task not found");
        }
        eventRepository.add(airdropId, "TASK_REMOVED", "Task removed");
    }

    private Airdrop editableAirdrop(String companyId, String airdropId) throws Exception {
        Airdrop airdrop = airdropRepository.findByIdAndCompany(airdropId, companyId)
                .orElseThrow(() -> new AirdropNotFoundException("Airdrop not found"));
        if (airdrop.status() != AirdropStatus.DRAFT) {
            throw new InvalidAirdropStateException("Tasks can only be changed while the airdrop is DRAFT");
        }
        if (airdrop.claimsOpen()) {
            throw new InvalidAirdropStateException("Close claims before changing tasks");
        }
        return airdrop;
    }

    static TaskType parseType(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Task type is required (QUIZ, SECRET_CODE or MANUAL_PROOF)");
        }
        try {
            return TaskType.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown task type '" + text + "' (use QUIZ, SECRET_CODE or MANUAL_PROOF)");
        }
    }
}
