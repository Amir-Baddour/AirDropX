package org.example.Core.Claim.Provider;

import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Airdrop.Model.AirdropStatus;
import org.example.Core.Claim.Exception.ClaimNotFoundException;
import org.example.Core.Claim.Helper.ClaimTokens;
import org.example.Core.Claim.Model.Claim;
import org.example.Core.Claim.Model.ClaimStatus;
import org.example.Core.Claim.Model.PublicClaimStatus;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Infra.Persistence.Airdrop.AirdropRepository;
import org.example.Infra.Persistence.Claim.ClaimRepository;
import org.example.Infra.Persistence.Company.CompanyRepository;
import org.example.Infra.Persistence.Task.TaskRepository;

import java.util.List;
import java.util.Map;

public class ClaimProvider {
    private final AirdropRepository airdropRepository;
    private final ClaimRepository claimRepository;
    private final TaskRepository taskRepository;
    private final CompanyRepository companyRepository;

    public ClaimProvider() {
        this.airdropRepository = new AirdropRepository();
        this.claimRepository = new ClaimRepository();
        this.taskRepository = new TaskRepository();
        this.companyRepository = new CompanyRepository();
    }

    public List<Claim> listClaims(String companyId, String airdropId, String status, int limit, int offset) throws Exception {
        requireOwned(companyId, airdropId);
        String cleanStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                cleanStatus = ClaimStatus.valueOf(status.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown claim status '" + status + "'");
            }
        }
        return claimRepository.listByAirdrop(airdropId, cleanStatus, Math.max(1, Math.min(limit, 200)), Math.max(0, offset));
    }

    public Map<String, Long> countsByStatus(String companyId, String airdropId) throws Exception {
        requireOwned(companyId, airdropId);
        return claimRepository.countsByStatus(airdropId);
    }

    /** The public campaign: only visible while claims are open. */
    public Airdrop getOpenAirdrop(String airdropId) throws Exception {
        Airdrop airdrop = airdropRepository.findById(airdropId)
                .filter(a -> a.claimsOpen() && a.status() == AirdropStatus.DRAFT)
                .orElseThrow(() -> new AirdropNotFoundException("This airdrop is not accepting claims"));
        if (!companyRepository.isActive(airdrop.companyId())) {
            throw new AirdropNotFoundException("This airdrop is not accepting claims");
        }
        return airdrop;
    }

    public List<AirdropTask> getPublicTasks(String airdropId) throws Exception {
        return taskRepository.listByAirdrop(airdropId);
    }

    public PublicClaimStatus getByToken(String token) throws Exception {
        if (!ClaimTokens.looksLikeToken(token)) {
            throw new ClaimNotFoundException("Claim not found");
        }
        return claimRepository.findPublicStatus(ClaimTokens.sha256(token))
                .orElseThrow(() -> new ClaimNotFoundException("Claim not found"));
    }

    private void requireOwned(String companyId, String airdropId) throws Exception {
        airdropRepository.findByIdAndCompany(airdropId, companyId)
                .orElseThrow(() -> new AirdropNotFoundException("Airdrop not found"));
    }
}
