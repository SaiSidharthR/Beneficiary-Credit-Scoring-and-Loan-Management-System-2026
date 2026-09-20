package com.saisidharth.loanmanagement.loanapplication;

import java.math.BigDecimal;

public record LoanRepaymentRequest(BigDecimal amount, String paymentMode, String reference) {
}
