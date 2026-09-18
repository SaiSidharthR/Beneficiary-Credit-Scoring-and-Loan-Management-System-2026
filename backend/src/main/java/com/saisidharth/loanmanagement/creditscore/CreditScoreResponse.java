package com.saisidharth.loanmanagement.creditscore;

import java.util.UUID;

public record CreditScoreResponse(
        UUID beneficiaryId,
        int score,
        boolean eligible,
        String rating) {
}