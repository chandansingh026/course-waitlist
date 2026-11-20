package com.example.coursewaitlist.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.coursewaitlist.application.EnrollmentOperations;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@WebMvcTest(RegistrationController.class)
class ApiFailureHandlingTest {
    private static final String PATH = "/api/v1/offerings/CS101-FALL/registrations";
    private static final String INTERNAL_DETAIL = "private-internal-diagnostic";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EnrollmentOperations enrollment;

    @ParameterizedTest
    @MethodSource("internalFailures")
    void unexpectedFailuresAreSanitized(RuntimeException failure) throws Exception {
        when(enrollment.register("CS101-FALL", "a")).thenThrow(failure);

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"learnerId\":\"a\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:course-waitlist:internal-error"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.instance").value(PATH))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString(INTERNAL_DETAIL))));
    }

    static Stream<RuntimeException> internalFailures() {
        return Stream.of(new IllegalStateException(INTERNAL_DETAIL),
                new UnsupportedOperationException(INTERNAL_DETAIL),
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_DETAIL));
    }
}
