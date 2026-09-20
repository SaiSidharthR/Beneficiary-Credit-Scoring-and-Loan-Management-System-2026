package com.saisidharth.loanmanagement.loanapplication;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanRepaymentSummary(
        UUID loanId,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance) {
}
