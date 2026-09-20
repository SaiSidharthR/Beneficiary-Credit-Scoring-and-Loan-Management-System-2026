package com.saisidharth.loanmanagement.loanapplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;
    private final LoanRepaymentRepository repaymentRepository;

    public LoanApplicationService(LoanApplicationRepository repository, LoanRepaymentRepository repaymentRepository) {
        this.repository = repository;
        this.repaymentRepository = repaymentRepository;
    }

    public LoanApplication create(LoanApplicationRequest request) {
        validate(request);

        Instant now = Instant.now();
        return repository.save(new LoanApplication(
                UUID.randomUUID(),
                UUID.fromString(request.beneficiaryId()),
                request.amount(),
                request.termMonths(),
                request.purpose().trim(),
                LoanApplicationStatus.PENDING,
                null,
                now,
                now));
    }

    public List<LoanApplication> findAll() {
        return repository.findAll();
    }

    public LoanApplication findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new LoanApplicationNotFoundException(id));
    }

    public LoanApplication update(UUID id, LoanApplicationRequest request) {
        validate(request);
        LoanApplication current = findById(id);
        return repository.save(new LoanApplication(
                current.id(),
                UUID.fromString(request.beneficiaryId()),
                request.amount(),
                request.termMonths(),
                request.purpose().trim(),
                current.status(),
                current.reviewNotes(),
                current.createdAt(),
                Instant.now()));
    }

    public LoanApplication updateStatus(UUID id, LoanStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("status is required");
        }

        LoanApplication current = findById(id);
        LoanApplication updated = new LoanApplication(
                current.id(),
                current.beneficiaryId(),
                current.amount(),
                current.termMonths(),
                current.purpose(),
                request.status(),
                request.reviewNotes() == null ? current.reviewNotes() : request.reviewNotes().trim(),
                current.createdAt(),
                Instant.now());

        return repository.save(updated);
    }

    public LoanRepaymentSchedule createSchedule(UUID id) {
        LoanApplication loan = findById(id);
        BigDecimal principal = loan.amount();
        BigDecimal monthlyEmi = principal.divide(BigDecimal.valueOf(loan.termMonths()), 2, java.math.RoundingMode.HALF_UP);
        return new LoanRepaymentSchedule(loan.id(), principal, loan.termMonths(), monthlyEmi);
    }

    public LoanRepayment createRepayment(UUID loanId, LoanRepaymentRequest request) {
        if (request == null || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0
                || isBlank(request.paymentMode()) || isBlank(request.reference())) {
            throw new IllegalArgumentException("amount, paymentMode, and reference are required");
        }

        findById(loanId);
        Instant now = Instant.now();
        return repaymentRepository.save(new LoanRepayment(
                UUID.randomUUID(),
                loanId,
                request.amount(),
                request.paymentMode().trim(),
                request.reference().trim(),
                now));
    }

    public LoanRepaymentSummary findSummary(UUID loanId) {
        LoanApplication loan = findById(loanId);
        BigDecimal totalPaid = repaymentRepository.findByLoanId(loanId).stream()
                .map(LoanRepayment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LoanRepaymentSummary(loan.id(), totalPaid, loan.amount().subtract(totalPaid));
    }

    public void delete(UUID id) {
        if (!repository.deleteById(id)) {
            throw new LoanApplicationNotFoundException(id);
        }
    }

    private void validate(LoanApplicationRequest request) {
        if (request == null || isBlank(request.beneficiaryId()) || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0
                || request.termMonths() == null || request.termMonths() <= 0 || isBlank(request.purpose())) {
            throw new IllegalArgumentException("beneficiaryId, amount, termMonths, and purpose are required");
        }

        try {
            UUID.fromString(request.beneficiaryId());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("beneficiaryId, amount, termMonths, and purpose are required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
