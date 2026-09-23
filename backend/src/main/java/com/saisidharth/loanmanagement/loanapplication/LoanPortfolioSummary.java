package com.saisidharth.loanmanagement.loanapplication;

import java.math.BigDecimal;

public record LoanPortfolioSummary(
        long totalApplications,
        long approvedApplications,
        long pendingApplications,
        long rejectedApplications,
        BigDecimal totalDisbursed,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance) {
}
