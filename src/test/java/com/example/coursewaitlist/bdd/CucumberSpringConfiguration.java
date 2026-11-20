package com.example.coursewaitlist.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@CucumberContextConfiguration
@ContextConfiguration(classes = CucumberSpringConfiguration.TestConfiguration.class)
public class CucumberSpringConfiguration {
    @Configuration
    static class TestConfiguration {
    }
}
