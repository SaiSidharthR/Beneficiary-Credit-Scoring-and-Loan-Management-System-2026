package com.saisidharth.loanmanagement.loanapplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanApplication(
        UUID id,
        UUID beneficiaryId,
        BigDecimal amount,
        Integer termMonths,
        String purpose,
        BigDecimal annualInterestRate,
        LoanApplicationStatus status,
        String reviewNotes,
        Instant createdAt,
        Instant updatedAt) {

        public LoanApplication(
                        UUID id,
                        UUID beneficiaryId,
                        BigDecimal amount,
                        Integer termMonths,
                        String purpose,
                        LoanApplicationStatus status,
                        String reviewNotes,
                        Instant createdAt,
                        Instant updatedAt) {
                this(id, beneficiaryId, amount, termMonths, purpose, BigDecimal.ZERO, status, reviewNotes, createdAt, updatedAt);
        }
}
