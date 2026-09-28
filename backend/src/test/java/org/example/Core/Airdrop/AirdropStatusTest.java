package org.example.Core.Airdrop;

import org.example.Core.Airdrop.Model.AirdropStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirdropStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(AirdropStatus.DRAFT.canTransitionTo(AirdropStatus.VALIDATED));
        assertTrue(AirdropStatus.VALIDATED.canTransitionTo(AirdropStatus.PROCESSING));
        assertTrue(AirdropStatus.PROCESSING.canTransitionTo(AirdropStatus.COMPLETED));
    }

    @Test
    void cannotSkipValidation() {
        assertFalse(AirdropStatus.DRAFT.canTransitionTo(AirdropStatus.PROCESSING));
        assertFalse(AirdropStatus.DRAFT.canTransitionTo(AirdropStatus.COMPLETED));
    }

    @Test
    void validatedCanGoBackToDraft() {
        assertTrue(AirdropStatus.VALIDATED.canTransitionTo(AirdropStatus.DRAFT));
        assertFalse(AirdropStatus.PROCESSING.canTransitionTo(AirdropStatus.DRAFT));
    }

    @Test
    void anyActiveStateCanBeCancelled() {
        assertTrue(AirdropStatus.DRAFT.canTransitionTo(AirdropStatus.CANCELLED));
        assertTrue(AirdropStatus.VALIDATED.canTransitionTo(AirdropStatus.CANCELLED));
        assertTrue(AirdropStatus.PROCESSING.canTransitionTo(AirdropStatus.CANCELLED));
    }

    @Test
    void terminalStatesAreFinal() {
        for (AirdropStatus next : AirdropStatus.values()) {
            assertFalse(AirdropStatus.COMPLETED.canTransitionTo(next));
            assertFalse(AirdropStatus.CANCELLED.canTransitionTo(next));
        }
        assertTrue(AirdropStatus.COMPLETED.isTerminal());
        assertTrue(AirdropStatus.CANCELLED.isTerminal());
        assertFalse(AirdropStatus.PROCESSING.isTerminal());
    }
}
