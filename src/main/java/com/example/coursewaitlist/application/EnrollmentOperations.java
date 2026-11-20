package com.example.coursewaitlist.application;

public interface EnrollmentOperations {
    RegistrationView register(String offeringId, String learnerId);
    RegistrationView getRegistration(String offeringId, String learnerId);
    DropResult drop(String offeringId, String learnerId);
}
