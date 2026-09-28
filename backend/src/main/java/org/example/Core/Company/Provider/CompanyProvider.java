package org.example.Core.Company.Provider;

import org.example.Core.Company.Exception.CompanyNotFoundException;
import org.example.Core.Company.Model.Company;
import org.example.Infra.Persistence.Company.CompanyRepository;

public class CompanyProvider {
    private final CompanyRepository companyRepository;

    public CompanyProvider() {
        this.companyRepository = new CompanyRepository();
    }

    /** The company the user belongs to; every airdrop call is scoped to it (tenant isolation). */
    public Company getCompanyOfUser(String userId) throws Exception {
        Company company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new CompanyNotFoundException("You don't belong to a company yet. Create one with POST /companies"));
        if (!"ACTIVE".equals(company.status())) {
            throw new CompanyNotFoundException("Your company is " + company.status().toLowerCase());
        }
        return company;
    }
}
