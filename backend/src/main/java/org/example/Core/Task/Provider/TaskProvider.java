package org.example.Core.Task.Provider;

import com.google.gson.JsonObject;
import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Verifier.TaskVerifierRegistry;
import org.example.Infra.Persistence.Airdrop.AirdropRepository;
import org.example.Infra.Persistence.Task.TaskRepository;

import java.util.List;

public class TaskProvider {
    private final AirdropRepository airdropRepository;
    private final TaskRepository taskRepository;
    private final TaskVerifierRegistry registry;

    public TaskProvider() {
        this.airdropRepository = new AirdropRepository();
        this.taskRepository = new TaskRepository();
        this.registry = new TaskVerifierRegistry();
    }

    /** Company view: full config (quiz answers included; secret codes are only stored as hashes). */
    public List<AirdropTask> listTasks(String companyId, String airdropId) throws Exception {
        airdropRepository.findByIdAndCompany(airdropId, companyId)
                .orElseThrow(() -> new AirdropNotFoundException("Airdrop not found"));
        return taskRepository.listByAirdrop(airdropId);
    }

    /** Claimant view of a task: never includes answers or secrets. */
    public JsonObject publicConfig(AirdropTask task) {
        return registry.forType(task.type()).publicConfig(task);
    }
}
