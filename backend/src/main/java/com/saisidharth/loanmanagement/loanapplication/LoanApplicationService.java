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
                interestRateOrZero(request),
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

        return repository.save(updated);
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

    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, java.math.RoundingMode.HALF_UP);
        }

        BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate).pow(termMonths);
        return principal.multiply(monthlyRate).multiply(growthFactor)
                .divide(growthFactor.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
    }
}
