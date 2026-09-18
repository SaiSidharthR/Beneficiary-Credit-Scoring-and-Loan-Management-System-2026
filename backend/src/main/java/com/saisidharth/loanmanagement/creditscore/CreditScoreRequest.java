package com.saisidharth.loanmanagement.creditscore;

import java.math.BigDecimal;

public record CreditScoreRequest(
        BigDecimal monthlyIncome,
        BigDecimal monthlyDebt,
        Integer onTimePayments,
        Integer missedPayments) {
}