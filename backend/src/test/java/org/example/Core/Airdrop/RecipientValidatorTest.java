package org.example.Core.Airdrop;

import org.example.Core.Airdrop.Model.RecipientInput;
import org.example.Core.Airdrop.Validator.RecipientValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipientValidatorTest {
    private static final String EVM_A = "0x1111111111111111111111111111111111111111";
    private static final String EVM_B = "0x2222222222222222222222222222222222222222";
    private static final String SOLANA = "9xQeWvG816bUx9EPjHmaT23yvVM2ZWbrrpZb9PusVFin";

    private final RecipientValidator validator = new RecipientValidator();

    private static RecipientInput r(String address, String amount) {
        return new RecipientInput(address, amount == null ? null : new BigDecimal(amount));
    }

    @Test
    void validListHasNoErrors() {
        List<String> errors = validator.validate(List.of(r(EVM_A, "10"), r(EVM_B, "0.5"), r(SOLANA, "1.000000000000000001")));
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void emptyListIsRejected() {
        assertEquals(1, validator.validate(List.of()).size());
        assertEquals(1, validator.validate(null).size());
    }

    @Test
    void invalidAddressIsRejected() {
        List<String> errors = validator.validate(List.of(r("0x123", "1"), r("not-an-address", "1")));
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).startsWith("Row 1"));
    }

    @Test
    void duplicateEvmAddressIsCaseInsensitive() {
        List<String> errors = validator.validate(List.of(
                r("0xabcdefabcdefabcdefabcdefabcdefabcdefabcd", "1"),
                r("0xABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCD", "2")));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("duplicate"));
    }

    @Test
    void amountMustBePositiveAndPresent() {
        List<String> errors = validator.validate(List.of(r(EVM_A, "0"), r(EVM_B, "-5"), r(SOLANA, null)));
        assertEquals(3, errors.size());
    }

    @Test
    void amountPrecisionIsLimitedTo18Decimals() {
        assertFalse(validator.validate(List.of(r(EVM_A, "0.0000000000000000001"))).isEmpty());
        assertTrue(validator.validate(List.of(r(EVM_A, "1.100000000000000000000"))).isEmpty());
    }

    @Test
    void tooManyRecipientsIsRejected() {
        List<RecipientInput> many = new ArrayList<>();
        for (int i = 0; i <= RecipientValidator.MAX_RECIPIENTS_PER_REQUEST; i++) {
            many.add(r(EVM_A, "1"));
        }
        List<String> errors = validator.validate(many);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Too many"));
    }

    @Test
    void normalizeLowercasesOnlyEvm() {
        assertEquals("0xabcdef", RecipientValidator.normalize(" 0xABCDEF "));
        assertEquals(SOLANA, RecipientValidator.normalize(SOLANA));
    }
}
