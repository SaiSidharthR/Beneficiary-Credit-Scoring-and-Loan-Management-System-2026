package com.saisidharth.loanmanagement.beneficiary;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class BeneficiaryRepository {

    private final Map<UUID, Beneficiary> beneficiaries = new ConcurrentHashMap<>();

    public Beneficiary save(Beneficiary beneficiary) {
        beneficiaries.put(beneficiary.id(), beneficiary);
        return beneficiary;
    }

    public List<Beneficiary> findAll() {
        return beneficiaries.values().stream()
                .sorted(Comparator.comparing(Beneficiary::createdAt).reversed())
                .toList();
    }

    public Optional<Beneficiary> findById(UUID id) {
        return Optional.ofNullable(beneficiaries.get(id));
    }

    public boolean deleteById(UUID id) {
        return beneficiaries.remove(id) != null;
    }
}
