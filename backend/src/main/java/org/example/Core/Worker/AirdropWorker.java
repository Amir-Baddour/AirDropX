package org.example.Core.Worker;

import org.example.Core.Airdrop.Model.AirdropStatus;
import org.example.Core.Airdrop.Model.Recipient;
import org.example.Core.Airdrop.Model.RecipientStatus;
import org.example.Core.Payout.PayoutProvider;
import org.example.Core.Payout.PayoutResult;
import org.example.Infra.JdbcConnection;
import org.example.Infra.Persistence.Airdrop.AirdropEventRepository;
import org.example.Infra.Persistence.Airdrop.AirdropRepository;
import org.example.Infra.Persistence.Airdrop.RecipientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background worker that sends payouts for PROCESSING airdrops.
 * Each tick: claim a batch of pending recipients (SKIP LOCKED), pay them, record results,
 * then mark airdrops with nothing left to send as COMPLETED.
 */
public class AirdropWorker {
    private static final Logger logger = LoggerFactory.getLogger(AirdropWorker.class.getName());
    private final RecipientRepository recipientRepository;
    private final AirdropRepository airdropRepository;
    private final AirdropEventRepository eventRepository;
    private final PayoutProvider payoutProvider;
    private final int batchSize;
    private final long intervalSeconds;
    private final Map<String, String> tokenSymbolCache = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    public AirdropWorker(PayoutProvider payoutProvider, int batchSize, long intervalSeconds) {
        this.recipientRepository = new RecipientRepository();
        this.airdropRepository = new AirdropRepository();
        this.eventRepository = new AirdropEventRepository();
        this.payoutProvider = payoutProvider;
        this.batchSize = batchSize;
        this.intervalSeconds = intervalSeconds;
    }

    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        try {
            int requeued = recipientRepository.requeueStuck(5);
            if (requeued > 0) {
                logger.warn("Re-queued {} recipient(s) left in PROCESSING by a previous run", requeued);
            }
        } catch (SQLException e) {
            logger.error("Could not re-queue stuck recipients: {}", e.getMessage());
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "airdrop-worker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::tickSafely, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        logger.info("Airdrop worker started (batch size {}, every {}s)", batchSize, intervalSeconds);
    }

    public synchronized void stop() {
        if (scheduler == null) {
            return;
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        scheduler = null;
        logger.info("Airdrop worker stopped");
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Exception e) {
            logger.error("Airdrop worker tick failed: {}", e.getMessage(), e);
        }
    }

    void tick() throws SQLException {
        List<Recipient> batch;
        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                batch = recipientRepository.claimPending(conn, batchSize);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        for (Recipient recipient : batch) {
            PayoutResult result;
            try {
                result = payoutProvider.send(recipient.address(), recipient.amount(), tokenSymbol(recipient.airdropId()));
            } catch (Exception e) {
                result = PayoutResult.failed("Payout error: " + e.getMessage());
            }
            if (result.success()) {
                recipientRepository.markResult(recipient.id(), RecipientStatus.COMPLETED, result.txRef(), null);
            } else {
                recipientRepository.markResult(recipient.id(), RecipientStatus.FAILED, null, result.error());
            }
        }

        completeFinishedAirdrops();
    }

    private void completeFinishedAirdrops() throws SQLException {
        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                for (String airdropId : airdropRepository.findFinishedProcessingIds(conn)) {
                    if (airdropRepository.transition(conn, airdropId, EnumSet.of(AirdropStatus.PROCESSING), AirdropStatus.COMPLETED)) {
                        eventRepository.add(conn, airdropId, "COMPLETED", "All payouts processed");
                        tokenSymbolCache.remove(airdropId);
                        logger.info("Airdrop {} completed", airdropId);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private String tokenSymbol(String airdropId) {
        return tokenSymbolCache.computeIfAbsent(airdropId, id -> {
            try {
                return airdropRepository.findTokenSymbol(id).orElse("");
            } catch (SQLException e) {
                return "";
            }
        });
    }
}
