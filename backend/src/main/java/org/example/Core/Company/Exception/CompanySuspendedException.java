package org.example.Core.Company.Exception;

/**
 * The user's company was suspended by a platform admin. Extends CompanyNotFoundException so every
 * company-scoped endpoint keeps answering 403; /companies/me reports it explicitly.
 */
public class CompanySuspendedException extends CompanyNotFoundException {
    private final String reason;

    public CompanySuspendedException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
