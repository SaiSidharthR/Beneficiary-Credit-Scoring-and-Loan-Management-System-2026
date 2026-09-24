package com.saisidharth.loanmanagement.loanapplication;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class LoanApplicationRepository {

    private final Map<UUID, LoanApplication> loanApplications = new ConcurrentHashMap<>();

    public LoanApplication save(LoanApplication loanApplication) {
        loanApplications.put(loanApplication.id(), loanApplication);
        return loanApplication;
    }

    public List<LoanApplication> findAll() {
        return loanApplications.values().stream()
                .sorted(Comparator.comparing(LoanApplication::createdAt).reversed())
                .toList();
    }

    public List<LoanApplication> findAll(LoanApplicationStatus status, UUID beneficiaryId, String purpose) {
        return loanApplications.values().stream()
                .filter(loan -> status == null || loan.status() == status)
                .filter(loan -> beneficiaryId == null || loan.beneficiaryId().equals(beneficiaryId))
                .filter(loan -> purpose == null || loan.purpose().toLowerCase().contains(purpose.toLowerCase()))
                .sorted(Comparator.comparing(LoanApplication::createdAt).reversed())
                .toList();
    }

    public Optional<LoanApplication> findById(UUID id) {
        return Optional.ofNullable(loanApplications.get(id));
    }

    public boolean deleteById(UUID id) {
        return loanApplications.remove(id) != null;
    }
}
