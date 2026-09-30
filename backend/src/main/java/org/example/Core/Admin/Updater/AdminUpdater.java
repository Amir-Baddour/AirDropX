package org.example.Core.Admin.Updater;

import org.example.Core.Company.Exception.CompanyNotFoundException;
import org.example.Core.Company.Exception.InvalidCompanyStateException;
import org.example.Infra.JdbcConnection;
import org.example.Infra.Persistence.Admin.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * Suspending a company freezes it: its members get 403 on every company endpoint, its public claim
 * pages disappear, and the payout worker skips its airdrops. Restoring resumes everything as it was.
 * Every change is written to the admin audit log in the same transaction.
 */
public class AdminUpdater {
    private static final Logger logger = LoggerFactory.getLogger(AdminUpdater.class.getName());
    private final AdminRepository adminRepository;

    public AdminUpdater() {
        this.adminRepository = new AdminRepository();
    }

    public void suspendCompany(String adminId, String companyId, String reason) throws Exception {
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) {
            throw new IllegalArgumentException("A reason is required (max 500 characters)");
        }
        change(adminId, companyId, "ACTIVE", "SUSPENDED", reason.trim(), "COMPANY_SUSPENDED", reason.trim());
    }

    public void restoreCompany(String adminId, String companyId, String note) throws Exception {
        if (note != null && note.length() > 500) {
            throw new IllegalArgumentException("Note is too long (max 500 characters)");
        }
        change(adminId, companyId, "SUSPENDED", "ACTIVE", null, "COMPANY_RESTORED",
                note == null || note.isBlank() ? null : note.trim());
    }

    private void change(String adminId, String companyId, String from, String to, String reason,
                        String action, String detail) throws Exception {
        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                String name = adminRepository.findCompanyName(conn, companyId)
                        .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
                if (!adminRepository.setCompanyStatus(conn, companyId, from, to, reason)) {
                    throw new InvalidCompanyStateException("Company is not " + from.toLowerCase());
                }
                adminRepository.addAudit(conn, adminId, action, "COMPANY", companyId,
                        name + (detail != null ? ": " + detail : ""));
                conn.commit();
                logger.info("Admin {} {} company {}", adminId, action, companyId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
