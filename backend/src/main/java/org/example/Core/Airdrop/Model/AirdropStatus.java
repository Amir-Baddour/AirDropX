package org.example.Core.Airdrop.Model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Airdrop lifecycle: DRAFT -> VALIDATED -> PROCESSING -> COMPLETED
 * CANCELLED can be reached from any non-terminal state.
 */
public enum AirdropStatus {
    DRAFT,
    VALIDATED,
    PROCESSING,
    COMPLETED,
    CANCELLED;

    public Set<AirdropStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> EnumSet.of(VALIDATED, CANCELLED);
            case VALIDATED -> EnumSet.of(DRAFT, PROCESSING, CANCELLED);
            case PROCESSING -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(AirdropStatus.class);
        };
    }

    public boolean canTransitionTo(AirdropStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
