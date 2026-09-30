package org.example.Core.Admin.Provider;

import org.example.Core.Admin.Model.AdminAction;
import org.example.Core.Admin.Model.CompanySummary;
import org.example.Core.Admin.Model.PlatformEvent;
import org.example.Core.Admin.Model.PlatformStats;
import org.example.Infra.Persistence.Admin.AdminRepository;

import java.util.List;

public class AdminProvider {
    private final AdminRepository adminRepository;

    public AdminProvider() {
        this.adminRepository = new AdminRepository();
    }

    public PlatformStats getStats() throws Exception {
        return adminRepository.stats();
    }

    public List<CompanySummary> listCompanies(String search, String status, int limit, int offset) throws Exception {
        String cleanStatus = null;
        if (status != null && !status.isBlank()) {
            cleanStatus = status.trim().toUpperCase();
            if (!cleanStatus.equals("ACTIVE") && !cleanStatus.equals("SUSPENDED")) {
                throw new IllegalArgumentException("status must be ACTIVE or SUSPENDED");
            }
        }
        if (search != null && search.length() > 100) {
            throw new IllegalArgumentException("Search text is too long");
        }
        return adminRepository.listCompanies(search, cleanStatus, clamp(limit, 1, 200), Math.max(0, offset));
    }

    public List<AdminAction> listAudit(int limit) throws Exception {
        return adminRepository.listAudit(clamp(limit, 1, 200));
    }

    public List<PlatformEvent> recentActivity(int limit) throws Exception {
        return adminRepository.recentEvents(clamp(limit, 1, 100));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
