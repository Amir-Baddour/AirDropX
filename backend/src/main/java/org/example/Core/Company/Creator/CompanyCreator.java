package org.example.Core.Company.Creator;

import org.example.Core.Company.Exception.CompanyAlreadyExistsException;
import org.example.Core.Company.Model.Company;
import org.example.Infra.Persistence.Company.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompanyCreator {
    private static final Logger logger = LoggerFactory.getLogger(CompanyCreator.class.getName());
    private final CompanyRepository companyRepository;

    public CompanyCreator() {
        this.companyRepository = new CompanyRepository();
    }

    public Company createCompany(String ownerId, String name) throws Exception {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 150) {
            throw new IllegalArgumentException("Company name is required (max 150 characters)");
        }
        if (companyRepository.findByUserId(ownerId).isPresent()) {
            throw new CompanyAlreadyExistsException("You already belong to a company");
        }
        Company company = companyRepository.createWithOwner(name.trim(), ownerId);
        logger.info("Company {} created by user {}", company.id(), ownerId);
        return company;
    }
}
