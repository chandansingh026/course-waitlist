package com.example.coursewaitlist.infrastructure.memory;

import com.example.coursewaitlist.application.OfferingRepository;
import com.example.coursewaitlist.domain.Offering;
import java.util.Optional;

public final class InMemoryOfferingRepository implements OfferingRepository {
    private final Offering offering;

    public InMemoryOfferingRepository(Offering offering) {
        this.offering = offering;
    }

    @Override
    public Optional<Offering> findById(String offeringId) {
        return offering.id().equals(offeringId) ? Optional.of(offering) : Optional.empty();
    }
}
