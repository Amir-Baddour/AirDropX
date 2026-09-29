package org.example.Core.Claim.Creator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Airdrop.Exception.InvalidAirdropStateException;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Airdrop.Model.AirdropStatus;
import org.example.Core.Airdrop.Model.RecipientInput;
import org.example.Core.Airdrop.Validator.RecipientValidator;
import org.example.Core.Claim.Exception.ClaimConflictException;
import org.example.Core.Claim.Exception.ClaimRejectedException;
import org.example.Core.Claim.Exception.TooManyClaimsException;
import org.example.Core.Claim.Helper.ClaimTokens;
import org.example.Core.Claim.Model.Claim;
import org.example.Core.Claim.Model.ClaimStatus;
import org.example.Core.Claim.Model.ClaimSubmission;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Model.TaskResult;
import org.example.Core.Task.Verifier.TaskVerifierRegistry;
import org.example.Infra.JdbcConnection;
import org.example.Infra.Persistence.Airdrop.AirdropEventRepository;
import org.example.Infra.Persistence.Airdrop.AirdropRepository;
import org.example.Infra.Persistence.Airdrop.RecipientRepository;
import org.example.Infra.Persistence.Claim.ClaimRepository;
import org.example.Infra.Persistence.Task.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Public claim submission. Runs in one transaction with the airdrop row locked, so the
 * max_claims limit and the "claims open" check cannot be raced.
 */
public class ClaimCreator {
    private static final Logger logger = LoggerFactory.getLogger(ClaimCreator.class.getName());
    public static final int MAX_CLAIMS_PER_IP = 3;
    private final AirdropRepository airdropRepository;
    private final TaskRepository taskRepository;
    private final ClaimRepository claimRepository;
    private final RecipientRepository recipientRepository;
    private final AirdropEventRepository eventRepository;
    private final TaskVerifierRegistry registry;

    public ClaimCreator() {
        this.airdropRepository = new AirdropRepository();
        this.taskRepository = new TaskRepository();
        this.claimRepository = new ClaimRepository();
        this.recipientRepository = new RecipientRepository();
        this.eventRepository = new AirdropEventRepository();
        this.registry = new TaskVerifierRegistry();
    }

    public ClaimSubmission submit(String airdropId, String address, JsonObject answers, String clientIp) throws Exception {
        if (address == null || !RecipientValidator.isValidAddress(address.trim())) {
            throw new IllegalArgumentException("Enter a valid wallet address (EVM 0x... or Solana)");
        }
        String cleanAddress = address.trim();
        String addressKey = RecipientValidator.normalize(cleanAddress);
        String ipHash = clientIp == null ? null : ClaimTokens.sha256(clientIp);

        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                Airdrop airdrop = airdropRepository.lockById(conn, airdropId)
                        .filter(a -> a.claimsOpen() && a.status() == AirdropStatus.DRAFT)
                        .orElseThrow(() -> new AirdropNotFoundException("This airdrop is not accepting claims"));

                List<AirdropTask> tasks = taskRepository.listByAirdrop(conn, airdropId);
                List<TaskResult> results = verifyAll(tasks, answers);
                List<String> failures = new ArrayList<>();
                boolean needsReview = false;
                for (int i = 0; i < results.size(); i++) {
                    TaskResult r = results.get(i);
                    if (!r.passed()) {
                        failures.add(tasks.get(i).title() + ": " + r.detail());
                    }
                    needsReview |= r.needsReview();
                }
                if (!failures.isEmpty()) {
                    throw new ClaimRejectedException("Some tasks are not completed", failures);
                }

                if (airdrop.maxClaims() != null && claimRepository.countActive(conn, airdropId) >= airdrop.maxClaims()) {
                    throw new InvalidAirdropStateException("This airdrop has reached its claim limit");
                }
                if (ipHash != null && claimRepository.countByIp(conn, airdropId, ipHash) >= MAX_CLAIMS_PER_IP) {
                    throw new TooManyClaimsException("Too many claims from your network for this airdrop");
                }

                ClaimStatus status = needsReview ? ClaimStatus.NEEDS_REVIEW : ClaimStatus.APPROVED;
                String token = ClaimTokens.newToken();
                String claimId = claimRepository.insert(conn, airdropId, cleanAddress, addressKey, status,
                                ClaimTokens.sha256(token), ipHash)
                        .orElseThrow(() -> new ClaimConflictException("This address has already claimed this airdrop"));
                claimRepository.insertResults(conn, claimId, results);
                if (status == ClaimStatus.APPROVED) {
                    recipientRepository.insertBatch(conn, airdropId, List.of(new RecipientInput(cleanAddress, airdrop.claimAmount())));
                }
                eventRepository.add(conn, airdropId, "CLAIM_" + status.name(), "Claim from " + shortAddress(cleanAddress));
                Claim claim = claimRepository.findById(conn, airdropId, claimId, false)
                        .orElseThrow(() -> new IllegalStateException("Claim vanished after insert"));
                conn.commit();
                logger.info("Claim {} on airdrop {} -> {}", claimId, airdropId, status);
                return new ClaimSubmission(claim, token);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Runs every task's verifier. Answers are keyed by task id. */
    List<TaskResult> verifyAll(List<AirdropTask> tasks, JsonObject answers) {
        List<TaskResult> results = new ArrayList<>(tasks.size());
        for (AirdropTask task : tasks) {
            JsonElement answer = answers == null ? null : answers.get(task.id());
            results.add(registry.forType(task.type()).verify(task, answer));
        }
        return results;
    }

    static String shortAddress(String address) {
        return address.length() <= 12 ? address : address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }
}
