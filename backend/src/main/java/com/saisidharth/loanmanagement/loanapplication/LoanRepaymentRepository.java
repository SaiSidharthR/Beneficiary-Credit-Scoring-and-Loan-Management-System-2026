package com.saisidharth.loanmanagement.loanapplication;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

@Repository
public class LoanRepaymentRepository {

    private final Map<UUID, LoanRepayment> repayments = new ConcurrentHashMap<>();

    public LoanRepayment save(LoanRepayment repayment) {
        repayments.put(repayment.id(), repayment);
        return repayment;
    }

    public List<LoanRepayment> findByLoanId(UUID loanId) {
        return repayments.values().stream()
                .filter(repayment -> repayment.loanId().equals(loanId))
                .sorted(Comparator.comparing(LoanRepayment::paidAt).reversed())
                .collect(Collectors.toList());
    }
}
