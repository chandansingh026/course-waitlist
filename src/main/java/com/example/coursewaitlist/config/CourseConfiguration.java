package com.example.coursewaitlist.config;

import com.example.coursewaitlist.application.EligibilityPolicy;
import com.example.coursewaitlist.application.EnrollmentOperations;
import com.example.coursewaitlist.application.EnrollmentService;
import com.example.coursewaitlist.application.OfferingRepository;
import com.example.coursewaitlist.application.RegistrationStore;
import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.infrastructure.memory.FixtureEligibilityPolicy;
import com.example.coursewaitlist.infrastructure.memory.InMemoryOfferingRepository;
import com.example.coursewaitlist.infrastructure.memory.InMemoryRegistrationStore;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CourseConfiguration {
    @Bean
    Offering offering(@Value("${course.offering-id}") String id, @Value("${course.capacity}") int capacity) {
        return new Offering(id, capacity);
    }

    @Bean
    OfferingRepository offeringRepository(Offering offering) {
        return new InMemoryOfferingRepository(offering);
    }

    @Bean
    RegistrationStore registrationStore(Offering offering) {
        return new InMemoryRegistrationStore(offering.id());
    }

    @Bean
    EligibilityPolicy eligibilityPolicy(@Value("${course.eligible-learner-ids:}") String ids) {
        Set<String> eligible = Arrays.stream(ids.split(","))
                .map(String::trim).filter(id -> !id.isEmpty()).collect(Collectors.toUnmodifiableSet());
        return new FixtureEligibilityPolicy(eligible);
    }

    @Bean
    EnrollmentOperations enrollmentOperations(OfferingRepository offerings, RegistrationStore store,
                                               EligibilityPolicy eligibility) {
        return new EnrollmentService(offerings, store, eligibility);
    }
}
