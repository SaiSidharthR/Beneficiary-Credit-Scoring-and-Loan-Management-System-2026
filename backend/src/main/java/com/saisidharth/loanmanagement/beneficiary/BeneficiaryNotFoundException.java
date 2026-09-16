package com.saisidharth.loanmanagement.beneficiary;

import java.util.UUID;

public class BeneficiaryNotFoundException extends RuntimeException {

    public BeneficiaryNotFoundException(UUID id) {
        super("Beneficiary not found: " + id);
    }
}
