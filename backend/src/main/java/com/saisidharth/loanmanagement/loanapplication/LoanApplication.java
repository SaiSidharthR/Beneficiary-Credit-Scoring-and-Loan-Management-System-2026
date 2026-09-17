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
        LoanApplicationStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
