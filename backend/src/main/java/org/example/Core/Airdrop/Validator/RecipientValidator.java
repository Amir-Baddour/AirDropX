package org.example.Core.Airdrop.Validator;

import org.example.Core.Airdrop.Model.RecipientInput;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure validation rules for recipient lists (no database access, easy to unit test).
 */
public class RecipientValidator {
    public static final int MAX_RECIPIENTS_PER_REQUEST = 10_000;
    public static final int MAX_AMOUNT_SCALE = 18;

    private static final Pattern EVM_ADDRESS = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    private static final Pattern SOLANA_ADDRESS = Pattern.compile("^[1-9A-HJ-NP-Za-km-z]{32,44}$");
    private static final int MAX_ERRORS_REPORTED = 50;

    public List<String> validate(List<RecipientInput> recipients) {
        List<String> errors = new ArrayList<>();
        if (recipients == null || recipients.isEmpty()) {
            errors.add("At least one recipient is required");
            return errors;
        }
        if (recipients.size() > MAX_RECIPIENTS_PER_REQUEST) {
            errors.add("Too many recipients in one request (max " + MAX_RECIPIENTS_PER_REQUEST + ")");
            return errors;
        }

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < recipients.size() && errors.size() < MAX_ERRORS_REPORTED; i++) {
            RecipientInput r = recipients.get(i);
            String row = "Row " + (i + 1) + ": ";
            if (r == null) {
                errors.add(row + "recipient is empty");
                continue;
            }
            String address = r.address() == null ? "" : r.address().trim();
            if (!isValidAddress(address)) {
                errors.add(row + "invalid address '" + address + "'");
            } else if (!seen.add(normalize(address))) {
                errors.add(row + "duplicate address '" + address + "'");
            }
            BigDecimal amount = r.amount();
            if (amount == null) {
                errors.add(row + "amount is required");
            } else if (amount.signum() <= 0) {
                errors.add(row + "amount must be greater than 0");
            } else if (amount.stripTrailingZeros().scale() > MAX_AMOUNT_SCALE) {
                errors.add(row + "amount has more than " + MAX_AMOUNT_SCALE + " decimal places");
            }
        }
        return errors;
    }

    public static boolean isValidAddress(String address) {
        return address != null && (EVM_ADDRESS.matcher(address).matches() || SOLANA_ADDRESS.matcher(address).matches());
    }

    /** EVM addresses are case-insensitive; Solana addresses are case-sensitive. */
    public static String normalize(String address) {
        String a = address.trim();
        return a.startsWith("0x") ? a.toLowerCase() : a;
    }
}
