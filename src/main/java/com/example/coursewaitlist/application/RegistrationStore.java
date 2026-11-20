package com.example.coursewaitlist.application;

import java.util.function.Function;

public interface RegistrationStore {
    <T> T execute(Function<RegistrationRepository, T> operation);
}
