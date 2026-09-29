package org.example.Core.Claim.Updater;

import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Airdrop.Exception.InvalidAirdropStateException;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Airdrop.Model.AirdropStatus;
import org.example.Core.Airdrop.Model.RecipientInput;
import org.example.Core.Airdrop.Validator.RecipientValidator;
import org.example.Core.Claim.Exception.ClaimConflictException;
import org.example.Core.Claim.Exception.ClaimNotFoundException;
import org.example.Core.Claim.Model.Claim;
import org.example.Core.Claim.Model.ClaimStatus;
import org.example.Infra.JdbcConnection;
import org.example.Infra.Persistence.Airdrop.AirdropEventRepository;
import org.example.Infra.Persistence.Airdrop.AirdropRepository;
import org.example.Infra.Persistence.Airdrop.RecipientRepository;
import org.example.Infra.Persistence.Claim.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public class ClaimUpdater {
    private static final Logger logger = LoggerFactory.getLogger(ClaimUpdater.class.getName());
    private final AirdropRepository airdropRepository;
    private final ClaimRepository claimRepository;
    private final RecipientRepository recipientRepository;
    private final AirdropEventRepository eventRepository;

    public ClaimUpdater() {
        this.airdropRepository = new AirdropRepository();
        this.claimRepository = new ClaimRepository();
        this.recipientRepository = new RecipientRepository();
        this.eventRepository = new AirdropEventRepository();
    }

    /** Opens the public claim page. Every approved claim becomes a recipient of {@code claimAmount}. */
    public Airdrop openClaims(String companyId, String airdropId, BigDecimal claimAmount, Integer maxClaims) throws Exception {
        Airdrop airdrop = draft(companyId, airdropId);
        BigDecimal amount = claimAmount != null ? claimAmount : airdrop.claimAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("claim_amount must be greater than 0");
        }
        if (amount.stripTrailingZeros().scale() > RecipientValidator.MAX_AMOUNT_SCALE) {
            throw new IllegalArgumentException("claim_amount has more than " + RecipientValidator.MAX_AMOUNT_SCALE + " decimals");
        }
        if (maxClaims != null && (maxClaims < 1 || maxClaims > RecipientValidator.MAX_RECIPIENTS_PER_REQUEST)) {
            throw new IllegalArgumentException("max_claims must be between 1 and " + RecipientValidator.MAX_RECIPIENTS_PER_REQUEST);
        }
        if (!airdropRepository.updateClaimSettings(airdropId, companyId, true, amount, maxClaims)) {
            throw new InvalidAirdropStateException("Airdrop changed status in the meantime; reload and try again");
        }
        eventRepository.add(airdropId, "CLAIMS_OPENED", "Public claims opened (" + amount.toPlainString() + " " + airdrop.tokenSymbol()
                + " per claim" + (maxClaims == null ? "" : ", max " + maxClaims) + ")");
        logger.info("Claims opened on airdrop {}", airdropId);
        return reload(companyId, airdropId);
    }

    public Airdrop closeClaims(String companyId, String airdropId) throws Exception {
        Airdrop airdrop = draft(companyId, airdropId);
        if (!airdrop.claimsOpen()) {
            return airdrop;
        }
        if (!airdropRepository.updateClaimSettings(airdropId, companyId, false, null, airdrop.maxClaims())) {
            throw new InvalidAirdropStateException("Airdrop changed status in the meantime; reload and try again");
        }
        eventRepository.add(airdropId, "CLAIMS_CLOSED", "Public claims closed");
        return reload(companyId, airdropId);
    }

    /** NEEDS_REVIEW -> APPROVED. The claimant is added as a recipient. */
    public Claim approve(String companyId, String userId, String airdropId, String claimId) throws Exception {
        return review(companyId, userId, airdropId, claimId, ClaimStatus.APPROVED, null);
    }

    /** NEEDS_REVIEW -> REJECTED. */
    public Claim reject(String companyId, String userId, String airdropId, String claimId, String reason) throws Exception {
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new IllegalArgumentException("A rejection reason is required (max 500 characters)");
        }
        return review(companyId, userId, airdropId, claimId, ClaimStatus.REJECTED, reason.trim());
    }

    private Claim review(String companyId, String userId, String airdropId, String claimId,
                         ClaimStatus to, String reason) throws Exception {
        draft(companyId, airdropId);
        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                Airdrop airdrop = airdropRepository.lockById(conn, airdropId)
                        .filter(a -> a.status() == AirdropStatus.DRAFT)
                        .orElseThrow(() -> new InvalidAirdropStateException("Claims can only be reviewed while the airdrop is DRAFT"));
                Claim claim = claimRepository.findById(conn, airdropId, claimId, true)
                        .orElseThrow(() -> new ClaimNotFoundException("Claim not found"));
                if (claim.status() != ClaimStatus.NEEDS_REVIEW) {
                    throw new ClaimConflictException("Claim is already " + claim.status());
                }
                claimRepository.updateStatus(conn, claimId, ClaimStatus.NEEDS_REVIEW, to, userId, reason);
                if (to == ClaimStatus.APPROVED) {
                    recipientRepository.insertBatch(conn, airdropId, List.of(new RecipientInput(claim.address(), airdrop.claimAmount())));
                }
                eventRepository.add(conn, airdropId, "CLAIM_" + to.name(), "Claim reviewed: " + to.name().toLowerCase());
                Claim updated = claimRepository.findById(conn, airdropId, claimId, false).orElseThrow();
                conn.commit();
                logger.info("Claim {} reviewed -> {}", claimId, to);
                return updated;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private Airdrop draft(String companyId, String airdropId) throws Exception {
        Airdrop airdrop = reload(companyId, airdropId);
        if (airdrop.status() != AirdropStatus.DRAFT) {
            throw new InvalidAirdropStateException("Claims can only be managed while the airdrop is DRAFT (current: " + airdrop.status() + ")");
        }
        return airdrop;
    }

    private Airdrop reload(String companyId, String airdropId) throws Exception {
        return airdropRepository.findByIdAndCompany(airdropId, companyId)
                .orElseThrow(() -> new AirdropNotFoundException("Airdrop not found"));
    }
}
