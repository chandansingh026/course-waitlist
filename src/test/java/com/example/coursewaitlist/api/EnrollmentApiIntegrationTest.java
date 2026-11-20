package com.example.coursewaitlist.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.coursewaitlist.application.RegistrationStore;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"course.capacity=1", "course.eligible-learner-ids=a,b,c"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EnrollmentApiIntegrationTest {
    private static final String OFFERING = "CS101-FALL";
    private static final String BASE = "/api/v1/offerings/" + OFFERING;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RegistrationStore store;

    @Test
    void enrollmentDuplicatesAndFifoDropsUpdateStoredState() throws Exception {
        register("a").andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ENROLLED"));
        register("b").andExpect(status().isCreated()).andExpect(jsonPath("$.waitlistPosition").value(1));
        register("c").andExpect(status().isCreated()).andExpect(jsonPath("$.waitlistPosition").value(2));
        register("a").andExpect(status().isOk()).andExpect(jsonPath("$.created").value(false));
        register("b").andExpect(status().isOk()).andExpect(jsonPath("$.waitlistPosition").value(1))
                .andExpect(jsonPath("$.created").value(false));

        assertEquals(List.of(row("a", "ENROLLED", null), row("b", "WAITLISTED", 1L),
                row("c", "WAITLISTED", 2L)), registrations());

        mvc.perform(delete(BASE + "/enrollments/a"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.promotedLearnerId").value("b"))
                .andExpect(jsonPath("$.remainingCapacity").value(0));
        mvc.perform(get(BASE + "/registrations/c"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.waitlistPosition").value(1));
        assertEquals(List.of(row("a", "DROPPED", null), row("b", "ENROLLED", null),
                row("c", "WAITLISTED", 2L)), registrations());

        register("a").andExpect(status().isCreated()).andExpect(jsonPath("$.waitlistPosition").value(2));
        mvc.perform(delete(BASE + "/enrollments/b"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.promotedLearnerId").value("c"));
        assertEquals(List.of(row("a", "WAITLISTED", 3L), row("b", "DROPPED", null),
                row("c", "ENROLLED", null)), registrations());
    }

    @Test
    void rejectedRequestsAndRepeatedDropDoNotCorruptStoredState() throws Exception {
        register("a").andExpect(status().isCreated());
        register("b").andExpect(status().isCreated());

        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":123}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        register("ineligible").andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value("urn:course-waitlist:ineligible"));
        mvc.perform(post("/api/v1/offerings/missing/registrations")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"learnerId\":\"a\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/enrollments/b")).andExpect(status().isConflict());
        mvc.perform(get(BASE + "/registrations/unknown")).andExpect(status().isNotFound());

        assertEquals(List.of(row("a", "ENROLLED", null), row("b", "WAITLISTED", 1L)), registrations());

        mvc.perform(delete(BASE + "/enrollments/a"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.promotedLearnerId").value("b"));
        mvc.perform(delete(BASE + "/enrollments/a"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertEquals(List.of(row("a", "DROPPED", null), row("b", "ENROLLED", null)), registrations());
    }

    private ResultActions register(String learner) throws Exception {
        return mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"learnerId\":\"" + learner + "\"}"));
    }

    private List<StoredRegistration> registrations() {
        return store.execute(repository -> Stream.of("a", "b", "c")
                .flatMap(learner -> repository.find(OFFERING, learner).stream())
                .map(registration -> row(registration.learnerId(), registration.status().name(),
                        registration.waitlistSequence())).toList());
    }

    private static StoredRegistration row(String learner, String status, Long sequence) {
        return new StoredRegistration(learner, status, sequence);
    }

    private record StoredRegistration(String learner, String status, Long sequence) {
    }
}
