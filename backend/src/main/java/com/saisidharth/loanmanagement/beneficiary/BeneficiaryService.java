package com.saisidharth.loanmanagement.beneficiary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository repository;

    public BeneficiaryService(BeneficiaryRepository repository) {
        this.repository = repository;
    }

    public Beneficiary create(BeneficiaryRequest request) {
        validate(request);
        Instant now = Instant.now();
        return repository.save(new Beneficiary(
                UUID.randomUUID(), request.name().trim(), request.email().trim(), request.phone().trim(), now, now));
    }

    public List<Beneficiary> findAll() {
        return repository.findAll();
    }

    public Beneficiary findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new BeneficiaryNotFoundException(id));
    }

    public Beneficiary update(UUID id, BeneficiaryRequest request) {
        validate(request);
        Beneficiary current = findById(id);
        return repository.save(new Beneficiary(
                current.id(), request.name().trim(), request.email().trim(), request.phone().trim(),
                current.createdAt(), Instant.now()));
    }

    public void delete(UUID id) {
        if (!repository.deleteById(id)) {
            throw new BeneficiaryNotFoundException(id);
        }
    }

    private void validate(BeneficiaryRequest request) {
        if (request == null || isBlank(request.name()) || isBlank(request.email()) || isBlank(request.phone())) {
            throw new IllegalArgumentException("name, email, and phone are required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
