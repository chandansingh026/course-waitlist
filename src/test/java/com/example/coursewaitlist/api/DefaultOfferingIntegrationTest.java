package com.example.coursewaitlist.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DefaultOfferingIntegrationTest {
    private static final String BASE = "/api/v1/offerings/CS101-FALL";

    @Autowired
    private MockMvc mvc;

    @Test
    void defaultOfferingEnrolsTenAndWaitlistsRemainingEligibleLearners() throws Exception {
        List<String> learners = new ArrayList<>(
                IntStream.rangeClosed(1001, 1019).mapToObj(id -> "learner-" + id).toList());
        learners.add("learner-1042");

        for (int i = 0; i < learners.size(); i++) {
            var result = mvc.perform(post(BASE + "/registrations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"learnerId\":\"" + learners.get(i) + "\"}"))
                    .andExpect(status().isCreated());
            if (i < 10) {
                result.andExpect(jsonPath("$.status").value("ENROLLED"));
            } else {
                result.andExpect(jsonPath("$.status").value("WAITLISTED"))
                        .andExpect(jsonPath("$.waitlistPosition").value(i - 9));
            }
        }

        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"learner-1020\"}"))
                .andExpect(status().is(422));
        mvc.perform(delete(BASE + "/enrollments/learner-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promotedLearnerId").value("learner-1011"))
                .andExpect(jsonPath("$.remainingCapacity").value(0));
    }
}
