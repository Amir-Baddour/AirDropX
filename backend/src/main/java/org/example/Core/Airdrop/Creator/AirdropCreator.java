package org.example.Core.Airdrop.Creator;

import org.example.Core.Airdrop.Exception.AirdropNotFoundException;
import org.example.Core.Airdrop.Exception.AirdropValidationException;
import org.example.Core.Airdrop.Exception.InvalidAirdropStateException;
import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Airdrop.Model.AirdropStatus;
import org.example.Core.Airdrop.Model.RecipientInput;
import org.example.Core.Airdrop.Validator.RecipientValidator;
import org.example.Infra.JdbcConnection;
import org.example.Infra.Persistence.Airdrop.AirdropEventRepository;
import org.example.Infra.Persistence.Airdrop.AirdropRepository;
import org.example.Infra.Persistence.Airdrop.RecipientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AirdropCreator {
    private static final Logger logger = LoggerFactory.getLogger(AirdropCreator.class.getName());
    private final AirdropRepository airdropRepository;
    private final RecipientRepository recipientRepository;
    private final AirdropEventRepository eventRepository;
    private final RecipientValidator recipientValidator;

    public AirdropCreator() {
        this.airdropRepository = new AirdropRepository();
        this.recipientRepository = new RecipientRepository();
        this.eventRepository = new AirdropEventRepository();
        this.recipientValidator = new RecipientValidator();
    }

    public Airdrop createAirdrop(String companyId, String userId, String name, String description, String tokenSymbol) throws Exception {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 150) {
            throw new IllegalArgumentException("Airdrop name is required (max 150 characters)");
        }
        if (tokenSymbol == null || !tokenSymbol.trim().matches("^[A-Za-z0-9]{1,15}$")) {
            throw new IllegalArgumentException("Token symbol is required (1-15 letters or digits)");
        }
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Description is too long (max 2000 characters)");
        }
        Airdrop airdrop = airdropRepository.create(companyId, name.trim(), description, tokenSymbol.trim().toUpperCase(), userId);
        eventRepository.add(airdrop.id(), "CREATED", "Airdrop created as draft");
        logger.info("Airdrop {} created for company {}", airdrop.id(), companyId);
        return airdrop;
    }

    /**
     * Adds recipients to a DRAFT airdrop. All rows are validated first; nothing is saved if any row is invalid.
     * @return number of new recipients (duplicates of existing addresses are skipped)
     */
    public int addRecipients(String companyId, String airdropId, List<RecipientInput> recipients) throws Exception {
        Airdrop airdrop = airdropRepository.findByIdAndCompany(airdropId, companyId)
                .orElseThrow(() -> new AirdropNotFoundException("Airdrop not found"));
        if (airdrop.status() != AirdropStatus.DRAFT) {
            throw new InvalidAirdropStateException("Recipients can only be added while the airdrop is DRAFT (current: " + airdrop.status() + ")");
        }
        List<String> errors = recipientValidator.validate(recipients);
        if (!errors.isEmpty()) {
            throw new AirdropValidationException("Recipient list is invalid", errors);
        }
        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                int inserted = recipientRepository.insertBatch(conn, airdropId, recipients);
                int skipped = recipients.size() - inserted;
                eventRepository.add(conn, airdropId, "RECIPIENTS_ADDED",
                        inserted + " recipient(s) added" + (skipped > 0 ? ", " + skipped + " already existed" : ""));
                conn.commit();
                return inserted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
