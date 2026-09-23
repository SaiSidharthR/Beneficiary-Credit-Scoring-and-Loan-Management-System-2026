package com.saisidharth.loanmanagement.loanapplication;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

@Repository
public class LoanAuditRepository {

    private final Map<UUID, List<LoanAuditEvent>> auditLogByLoan = new ConcurrentHashMap<>();

    public void record(UUID loanId, String eventType, String message) {
        auditLogByLoan.computeIfAbsent(loanId, ignored -> new java.util.ArrayList<>())
                .add(new LoanAuditEvent(eventType, message, Instant.now()));
    }

    public List<LoanAuditEvent> findByLoanId(UUID loanId) {
        return auditLogByLoan.getOrDefault(loanId, List.of()).stream()
                .sorted(Comparator.comparing(LoanAuditEvent::createdAt).reversed())
                .collect(Collectors.toList());
    }

    public List<LoanAuditEvent> findAll() {
        return auditLogByLoan.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(LoanAuditEvent::createdAt).reversed())
                .collect(Collectors.toList());
    }
}
