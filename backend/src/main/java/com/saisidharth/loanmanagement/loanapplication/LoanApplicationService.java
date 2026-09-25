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
    private final LoanAuditRepository auditRepository;

    public LoanApplicationService(LoanApplicationRepository repository,
            LoanRepaymentRepository repaymentRepository,
            LoanAuditRepository auditRepository) {
        this.repository = repository;
        this.repaymentRepository = repaymentRepository;
        this.auditRepository = auditRepository;
    }

    public LoanApplication create(LoanApplicationRequest request) {
        validate(request);

        Instant now = Instant.now();
        LoanApplication loan = new LoanApplication(
                UUID.randomUUID(),
                UUID.fromString(request.beneficiaryId()),
                request.amount(),
                request.termMonths(),
                request.purpose().trim(),
                interestRateOrZero(request),
                LoanApplicationStatus.PENDING,
                null,
                now,
                now);
        repository.save(loan);
        auditRepository.record(loan.id(), "CREATED", "Loan application created for beneficiary " + loan.beneficiaryId());
        return loan;
    }

    public List<LoanApplication> findAll() {
        return repository.findAll();
    }

    public List<LoanApplication> findAll(String status, String beneficiaryId, String purpose) {
        return findAll(status, beneficiaryId, purpose, 0, Integer.MAX_VALUE);
    }

    public List<LoanApplication> findAll(String status, String beneficiaryId, String purpose, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("page must be zero or greater and size must be greater than zero");
        }

        size = Math.min(size, 50);

        LoanApplicationStatus parsedStatus = parseStatus(status);
        UUID parsedBeneficiaryId = parseBeneficiaryId(beneficiaryId);
        String normalizedPurpose = purpose == null || purpose.isBlank() ? null : purpose.trim();
        List<LoanApplication> loans = repository.findAll(parsedStatus, parsedBeneficiaryId, normalizedPurpose);
        long offset = (long) page * size;
        if (offset >= loans.size()) {
            return List.of();
        }

        int fromIndex = (int) offset;
        int toIndex = (int) Math.min(offset + size, loans.size());
        return loans.subList(fromIndex, toIndex);
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
                interestRateOrZero(request),
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
        if (current.status() != LoanApplicationStatus.PENDING) {
            throw new IllegalArgumentException("only pending loans can be reviewed");
        }

        LoanApplication updated = new LoanApplication(
                current.id(),
                current.beneficiaryId(),
                current.amount(),
                current.termMonths(),
                current.purpose(),
                current.annualInterestRate(),
                request.status(),
                request.reviewNotes() == null ? current.reviewNotes() : request.reviewNotes().trim(),
                current.createdAt(),
                Instant.now());

        repository.save(updated);
        auditRepository.record(updated.id(), "STATUS_UPDATED", "Loan marked as " + updated.status() + " with review notes: " + (updated.reviewNotes() == null ? "none" : updated.reviewNotes()));
        return updated;
    }

    public LoanRepaymentSchedule createSchedule(UUID id) {
        LoanApplication loan = findById(id);
        BigDecimal principal = loan.amount();
        BigDecimal monthlyRate = loan.annualInterestRate()
            .divide(BigDecimal.valueOf(1200), 12, java.math.RoundingMode.HALF_UP);
        BigDecimal monthlyEmi = calculateEmi(principal, monthlyRate, loan.termMonths());
        return new LoanRepaymentSchedule(loan.id(), principal, loan.termMonths(), loan.annualInterestRate(), monthlyEmi);
    }

    public LoanRepayment createRepayment(UUID loanId, LoanRepaymentRequest request) {
        if (request == null || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0
                || isBlank(request.paymentMode()) || isBlank(request.reference())) {
            throw new IllegalArgumentException("amount, paymentMode, and reference are required");
        }

        LoanApplication loan = findById(loanId);
        if (loan.status() != LoanApplicationStatus.APPROVED) {
            throw new IllegalArgumentException("only approved loans can receive repayments");
        }

        BigDecimal totalPaid = repaymentRepository.findByLoanId(loanId).stream()
                .map(LoanRepayment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPaid.add(request.amount()).compareTo(loan.amount()) > 0) {
            throw new IllegalArgumentException("repayment exceeds outstanding balance");
        }

        Instant now = Instant.now();
        LoanRepayment repayment = repaymentRepository.save(new LoanRepayment(
                UUID.randomUUID(),
                loanId,
                request.amount(),
                request.paymentMode().trim(),
                request.reference().trim(),
                now));
        auditRepository.record(loanId, "REPAYMENT", "Recorded repayment of " + repayment.amount() + " via " + repayment.paymentMode());
        return repayment;
    }

    public LoanRepaymentSummary findSummary(UUID loanId) {
        LoanApplication loan = findById(loanId);
        BigDecimal totalPaid = repaymentRepository.findByLoanId(loanId).stream()
                .map(LoanRepayment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LoanRepaymentSummary(loan.id(), totalPaid, loan.amount().subtract(totalPaid));
    }

    public LoanPortfolioSummary getPortfolioSummary() {
        List<LoanApplication> loans = repository.findAll();
        long totalApplications = loans.size();
        long approvedApplications = loans.stream().filter(loan -> loan.status() == LoanApplicationStatus.APPROVED).count();
        long pendingApplications = loans.stream().filter(loan -> loan.status() == LoanApplicationStatus.PENDING).count();
        long rejectedApplications = loans.stream().filter(loan -> loan.status() == LoanApplicationStatus.REJECTED).count();

        BigDecimal totalDisbursed = loans.stream()
                .filter(loan -> loan.status() == LoanApplicationStatus.APPROVED)
                .map(LoanApplication::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = repaymentRepository.findAll().stream()
                .map(LoanRepayment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingBalance = totalDisbursed.subtract(totalPaid);
        return new LoanPortfolioSummary(
                totalApplications,
                approvedApplications,
                pendingApplications,
                rejectedApplications,
                totalDisbursed,
                totalPaid,
                outstandingBalance);
    }

    public List<LoanAuditEvent> getAuditLog(UUID loanId) {
        return auditRepository.findByLoanId(loanId);
    }

    public void delete(UUID id) {
        if (!repository.deleteById(id)) {
            throw new LoanApplicationNotFoundException(id);
        }
    }

    private void validate(LoanApplicationRequest request) {
        if (request == null || isBlank(request.beneficiaryId()) || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0
                || request.termMonths() == null || request.termMonths() <= 0 || isBlank(request.purpose())
                || request.annualInterestRate() != null && request.annualInterestRate().compareTo(BigDecimal.ZERO) < 0) {
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

    private BigDecimal interestRateOrZero(LoanApplicationRequest request) {
        return request.annualInterestRate() == null ? BigDecimal.ZERO : request.annualInterestRate();
    }

    private LoanApplicationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return LoanApplicationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status must be PENDING, APPROVED, or REJECTED");
        }
    }

    private UUID parseBeneficiaryId(String beneficiaryId) {
        if (beneficiaryId == null || beneficiaryId.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(beneficiaryId.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("beneficiaryId must be a valid UUID");
        }
    }

    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, java.math.RoundingMode.HALF_UP);
        }

        BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate).pow(termMonths);
        return principal.multiply(monthlyRate).multiply(growthFactor)
                .divide(growthFactor.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
    }
}
