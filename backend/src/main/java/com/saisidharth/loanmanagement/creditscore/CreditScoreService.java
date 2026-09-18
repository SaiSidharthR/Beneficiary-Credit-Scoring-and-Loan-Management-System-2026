package com.saisidharth.loanmanagement.creditscore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saisidharth.loanmanagement.beneficiary.BeneficiaryService;

@Service
public class CreditScoreService {

    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(850);
    private static final BigDecimal MIN_SCORE = BigDecimal.valueOf(300);

    private final BeneficiaryService beneficiaryService;

    public CreditScoreService(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    public CreditScoreResponse calculate(UUID beneficiaryId, CreditScoreRequest request) {
        beneficiaryService.findById(beneficiaryId);
        validate(request);

        BigDecimal totalPayments = BigDecimal.valueOf(request.onTimePayments() + request.missedPayments());
        BigDecimal paymentRate = totalPayments.signum() == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(request.onTimePayments()).divide(totalPayments, 6, RoundingMode.HALF_UP);
        BigDecimal debtRatio = request.monthlyDebt().divide(request.monthlyIncome(), 6, RoundingMode.HALF_UP);

        int score = MIN_SCORE
                .add(paymentRate.multiply(BigDecimal.valueOf(400)))
                .subtract(debtRatio.min(BigDecimal.ONE).multiply(BigDecimal.valueOf(150)))
                .setScale(0, RoundingMode.HALF_UP)
                .max(MIN_SCORE)
                .min(MAX_SCORE)
                .intValue();

        return new CreditScoreResponse(beneficiaryId, score, score >= 600, rating(score));
    }

    private void validate(CreditScoreRequest request) {
        if (request == null || request.monthlyIncome() == null || request.monthlyIncome().compareTo(BigDecimal.ZERO) <= 0
                || request.monthlyDebt() == null || request.monthlyDebt().compareTo(BigDecimal.ZERO) < 0
                || request.onTimePayments() == null || request.onTimePayments() < 0
                || request.missedPayments() == null || request.missedPayments() < 0) {
            throw new IllegalArgumentException(
                    "monthlyIncome, monthlyDebt, onTimePayments, and missedPayments must be valid");
        }
    }

    private String rating(int score) {
        if (score >= 750) {
            return "EXCELLENT";
        }
        if (score >= 650) {
            return "GOOD";
        }
        if (score >= 600) {
            return "FAIR";
        }
        return "POOR";
    }
}