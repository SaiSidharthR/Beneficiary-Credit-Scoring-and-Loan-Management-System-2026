package com.saisidharth.loanmanagement.beneficiary;

import java.time.Instant;
import java.util.UUID;

public record Beneficiary(
        UUID id,
        String name,
        String email,
        String phone,
        Instant createdAt,
        Instant updatedAt) {
}
