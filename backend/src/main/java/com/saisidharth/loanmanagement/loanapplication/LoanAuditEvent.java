package com.saisidharth.loanmanagement.loanapplication;

import java.time.Instant;

public record LoanAuditEvent(
        String eventType,
        String message,
        Instant createdAt) {
}
