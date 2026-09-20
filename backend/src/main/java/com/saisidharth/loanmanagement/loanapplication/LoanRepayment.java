package com.saisidharth.loanmanagement.loanapplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanRepayment(
        UUID id,
        UUID loanId,
        BigDecimal amount,
        String paymentMode,
        String reference,
        Instant paidAt) {
}
