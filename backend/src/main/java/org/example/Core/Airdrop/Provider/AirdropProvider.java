package org.example.Core.Airdrop.Provider;

import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Airdrop.Model.AirdropEvent;
import org.example.Core.Airdrop.Model.AirdropStats;
import org.example.Core.Airdrop.Model.Recipient;
import org.example.Core.Airdrop.Model.RecipientStatus;
import org.example.Infra.Persistence.Airdrop.AirdropEventRepository;
import org.example.Infra.Persistence.Airdrop.AirdropRepository;
import org.example.Infra.Persistence.Airdrop.RecipientRepository;

import java.util.List;

public class AirdropProvider {
    public static final int MAX_PAGE_SIZE = 100;
    private final AirdropRepository airdropRepository;
    private final RecipientRepository recipientRepository;
    private final AirdropEventRepository eventRepository;

    public AirdropProvider() {
        this.airdropRepository = new AirdropRepository();
        this.recipientRepository = new RecipientRepository();
        this.eventRepository = new AirdropEventRepository();
    }

    public Airdrop getAirdrop(String companyId, String airdropId) throws Exception {
        return airdropRepository.findByIdAndCompany(airdropId, companyId)
                .orElseThrow(() -> new AirdropNotFoundException("Airdrop not found"));
    }

    public List<Airdrop> listAirdrops(String companyId, int limit, int offset) throws Exception {
        return airdropRepository.listByCompany(companyId, clamp(limit), Math.max(offset, 0));
    }

    public AirdropStats getStats(String companyId, String airdropId) throws Exception {
        getAirdrop(companyId, airdropId);
        return recipientRepository.stats(airdropId);
    }

    public List<Recipient> listRecipients(String companyId, String airdropId, String status, int limit, int offset) throws Exception {
        getAirdrop(companyId, airdropId);
        String statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = RecipientStatus.valueOf(status.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown status '" + status + "'. Use one of: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED");
            }
        }
        return recipientRepository.listByAirdrop(airdropId, statusFilter, clamp(limit), Math.max(offset, 0));
    }

    public List<AirdropEvent> listEvents(String companyId, String airdropId) throws Exception {
        getAirdrop(companyId, airdropId);
        return eventRepository.listByAirdrop(airdropId, 200);
    }

    private static int clamp(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }
}
