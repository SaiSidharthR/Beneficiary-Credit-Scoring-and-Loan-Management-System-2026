package com.saisidharth.loanmanagement.loanapplication;

import java.util.UUID;

public class LoanApplicationNotFoundException extends RuntimeException {

    public LoanApplicationNotFoundException(UUID id) {
        super("Loan application not found: " + id);
    }
}
