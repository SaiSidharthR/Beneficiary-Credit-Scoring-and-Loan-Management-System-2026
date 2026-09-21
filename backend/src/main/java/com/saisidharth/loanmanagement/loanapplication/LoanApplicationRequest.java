package com.saisidharth.loanmanagement.loanapplication;

import java.math.BigDecimal;

public record LoanApplicationRequest(
		String beneficiaryId,
		BigDecimal amount,
		Integer termMonths,
		String purpose,
		BigDecimal annualInterestRate) {

	public LoanApplicationRequest(String beneficiaryId, BigDecimal amount, Integer termMonths, String purpose) {
		this(beneficiaryId, amount, termMonths, purpose, BigDecimal.ZERO);
	}
}
