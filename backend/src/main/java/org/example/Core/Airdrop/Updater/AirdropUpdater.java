package org.example.Core.Airdrop.Updater;

import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Airdrop.Exception.AirdropValidationException;
import org.example.Core.Airdrop.Exception.InvalidAirdropStateException;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Airdrop.Model.AirdropStatus;
import org.example.Infra.JdbcConnection;
import org.example.Infra.Persistence.Airdrop.AirdropEventRepository;
import org.example.Infra.Persistence.Airdrop.AirdropRepository;
import org.example.Infra.Persistence.Airdrop.RecipientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;

public class AirdropUpdater {
    private static final Logger logger = LoggerFactory.getLogger(AirdropUpdater.class.getName());
    private final AirdropRepository airdropRepository;
    private final RecipientRepository recipientRepository;
    private final AirdropEventRepository eventRepository;

    public AirdropUpdater() {
        this.airdropRepository = new AirdropRepository();
        this.recipientRepository = new RecipientRepository();
        this.eventRepository = new AirdropEventRepository();
    }

    /** DRAFT -> VALIDATED. Requires at least one recipient. */
    public Airdrop validate(String companyId, String airdropId) throws Exception {
        Airdrop airdrop = load(companyId, airdropId);
        requireTransition(airdrop, AirdropStatus.VALIDATED);
        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                long count = recipientRepository.countByAirdrop(conn, airdropId);
                if (count == 0) {
                    throw new AirdropValidationException("Add at least one recipient before validating");
                }
                move(conn, airdropId, EnumSet.of(AirdropStatus.DRAFT), AirdropStatus.VALIDATED);
                if (airdrop.claimsOpen()) {
                    airdropRepository.closeClaims(conn, airdropId);
                    eventRepository.add(conn, airdropId, "CLAIMS_CLOSED", "Public claims closed on validation");
                }
                eventRepository.add(conn, airdropId, "VALIDATED", "Validated with " + count + " recipient(s)");
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
        return load(companyId, airdropId);
    }

    /** VALIDATED -> DRAFT, so recipients can be edited again. */
    public Airdrop reopen(String companyId, String airdropId) throws Exception {
        return simpleMove(companyId, airdropId, AirdropStatus.VALIDATED, AirdropStatus.DRAFT, "REOPENED", "Moved back to draft");
    }

    /** VALIDATED -> PROCESSING. The background worker then sends the payouts. */
    public Airdrop launch(String companyId, String airdropId) throws Exception {
        return simpleMove(companyId, airdropId, AirdropStatus.VALIDATED, AirdropStatus.PROCESSING, "LAUNCHED", "Airdrop launched; payouts started");
    }

    /** Any non-terminal state -> CANCELLED. Recipients not yet paid are cancelled too. */
    public Airdrop cancel(String companyId, String airdropId) throws Exception {
        Airdrop airdrop = load(companyId, airdropId);
        requireTransition(airdrop, AirdropStatus.CANCELLED);
        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                move(conn, airdropId, EnumSet.of(AirdropStatus.DRAFT, AirdropStatus.VALIDATED, AirdropStatus.PROCESSING), AirdropStatus.CANCELLED);
                airdropRepository.closeClaims(conn, airdropId);
                int cancelled = recipientRepository.cancelOpen(conn, airdropId);
                eventRepository.add(conn, airdropId, "CANCELLED", "Airdrop cancelled; " + cancelled + " pending recipient(s) cancelled");
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
        logger.info("Airdrop {} cancelled", airdropId);
        return load(companyId, airdropId);
    }

    private Airdrop simpleMove(String companyId, String airdropId, AirdropStatus from, AirdropStatus to,
                               String eventType, String message) throws Exception {
        Airdrop airdrop = load(companyId, airdropId);
        requireTransition(airdrop, to);
        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                move(conn, airdropId, EnumSet.of(from), to);
                eventRepository.add(conn, airdropId, eventType, message);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
        logger.info("Airdrop {} moved {} -> {}", airdropId, from, to);
        return load(companyId, airdropId);
    }

    private void move(Connection conn, String airdropId, EnumSet<AirdropStatus> from, AirdropStatus to)
            throws SQLException, InvalidAirdropStateException {
        if (!airdropRepository.transition(conn, airdropId, from, to)) {
            throw new InvalidAirdropStateException("Airdrop changed status in the meantime; reload and try again");
        }
    }

    private Airdrop load(String companyId, String airdropId) throws Exception {
        return airdropRepository.findByIdAndCompany(airdropId, companyId)
                .orElseThrow(() -> new AirdropNotFoundException("Airdrop not found"));
    }

    private static void requireTransition(Airdrop airdrop, AirdropStatus to) throws InvalidAirdropStateException {
        if (!airdrop.status().canTransitionTo(to)) {
            throw new InvalidAirdropStateException("Cannot move airdrop from " + airdrop.status() + " to " + to);
        }
    }
}
