package com.example.coursewaitlist.application;

import com.example.coursewaitlist.domain.Offering;
import java.util.Optional;

public interface OfferingRepository {
    Optional<Offering> findById(String offeringId);
}
