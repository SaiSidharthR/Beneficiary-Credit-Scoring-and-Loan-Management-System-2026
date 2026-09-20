package com.saisidharth.loanmanagement.loanapplication;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoanApplicationController {

    private final LoanApplicationService service;

    public LoanApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    @PostMapping({"/loans", "/loan-applications"})
    public ResponseEntity<LoanApplication> create(@RequestBody LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping({"/loans", "/loan-applications"})
    public List<LoanApplication> findAll() {
        return service.findAll();
    }

    @GetMapping({"/loans/{id}", "/loan-applications/{id}"})
    public LoanApplication findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping({"/loans/{id}", "/loan-applications/{id}"})
    public LoanApplication update(@PathVariable UUID id, @RequestBody LoanApplicationRequest request) {
        return service.update(id, request);
    }

    @PatchMapping({"/loans/{id}/status", "/loan-applications/{id}/status"})
    public LoanApplication updateStatus(@PathVariable UUID id, @RequestBody LoanStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @GetMapping({"/loans/{id}/schedule", "/loan-applications/{id}/schedule"})
    public LoanRepaymentSchedule getSchedule(@PathVariable UUID id) {
        return service.createSchedule(id);
    }

    @PostMapping({"/loans/{id}/repayments", "/loan-applications/{id}/repayments"})
    public ResponseEntity<LoanRepayment> createRepayment(@PathVariable UUID id, @RequestBody LoanRepaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRepayment(id, request));
    }

    @GetMapping({"/loans/{id}/summary", "/loan-applications/{id}/summary"})
    public LoanRepaymentSummary getSummary(@PathVariable UUID id) {
        return service.findSummary(id);
    }

    @DeleteMapping({"/loans/{id}", "/loan-applications/{id}"})
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
