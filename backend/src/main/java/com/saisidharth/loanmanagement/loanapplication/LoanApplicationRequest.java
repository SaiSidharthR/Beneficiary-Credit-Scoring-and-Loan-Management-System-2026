package com.saisidharth.loanmanagement.loanapplication;

import java.math.BigDecimal;

public record LoanApplicationRequest(String beneficiaryId, BigDecimal amount, Integer termMonths, String purpose) {
}
