package com.saisidharth.loanmanagement.creditscore;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beneficiaries/{beneficiaryId}/credit-score")
public class CreditScoreController {

    private final CreditScoreService service;

    public CreditScoreController(CreditScoreService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreditScoreResponse> calculate(
            @PathVariable UUID beneficiaryId,
            @RequestBody CreditScoreRequest request) {
        return ResponseEntity.ok(service.calculate(beneficiaryId, request));
    }
}