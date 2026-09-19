package com.saisidharth.loanmanagement.loanapplication;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanRepaymentSchedule(
        UUID loanId,
        BigDecimal principal,
        Integer termMonths,
        BigDecimal monthlyEmi) {
}
