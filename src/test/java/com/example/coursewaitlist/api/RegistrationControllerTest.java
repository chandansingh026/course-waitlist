package com.example.coursewaitlist.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"course.capacity=1", "course.eligible-learner-ids=a,b"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RegistrationControllerTest {
    private static final String BASE = "/api/v1/offerings/CS101-FALL";

    @Autowired
    private MockMvc mvc;

    @Test
    void registrationAndPromotionLifecycle() throws Exception {
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"a\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ENROLLED"));
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"a\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENROLLED"))
                .andExpect(jsonPath("$.created").value(false));
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"b\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1));
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"b\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(false));
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"c\"}"))
                .andExpect(status().is(422));
        mvc.perform(delete(BASE + "/enrollments/b"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(get(BASE + "/registrations/b"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.waitlistPosition").value(1));
        mvc.perform(delete(BASE + "/enrollments/a"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.promotedLearnerId").value("b"));
        mvc.perform(get(BASE + "/registrations/b"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENROLLED"));
        mvc.perform(delete(BASE + "/enrollments/a"))
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void invalidAndIneligibleRequestsReturnProblems() throws Exception {
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"c\"}"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("urn:course-waitlist:ineligible"));
        mvc.perform(get("/api/v1/offerings/missing/registrations/a"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/offerings/missing/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"learnerId\":\"a\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.type").value("urn:course-waitlist:not_found"));
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(BASE + "/registrations/a"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.TEXT_PLAIN).content("not json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(415));
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void openApiDescribesSuccessAndProblemResponsesAndNullableFields() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.1.0"))
                .andExpect(jsonPath("$.paths['/api/v1/offerings/{offeringId}/registrations'].post.requestBody.required")
                        .value(true))
                .andExpect(jsonPath("$.paths['/api/v1/offerings/{offeringId}/registrations'].post.requestBody.content"
                        + "['application/json'].schema.$ref").value("#/components/schemas/RegistrationRequest"))
                .andExpect(jsonPath("$.components.schemas.RegistrationRequest.properties.learnerId.minLength").value(1))
                .andExpect(jsonPath("$.components.schemas.RegistrationRequest.properties.learnerId.maxLength").value(100))
                .andExpect(jsonPath("$.paths['/api/v1/offerings/{offeringId}/registrations'].post.responses"
                        + "['201'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/RegistrationView"))
                .andExpect(jsonPath("$.paths['/api/v1/offerings/{offeringId}/registrations'].post.responses"
                        + "['422'].content['application/problem+json'].schema.$ref")
                        .value("#/components/schemas/ApiProblem"))
                .andExpect(jsonPath("$.paths['/api/v1/offerings/{offeringId}/registrations/{learnerId}'].get.responses"
                        + "['404'].content['application/problem+json'].schema.$ref")
                        .value("#/components/schemas/ApiProblem"))
                .andExpect(jsonPath("$.paths['/api/v1/offerings/{offeringId}/enrollments/{learnerId}'].delete.responses"
                        + "['409'].content['application/problem+json'].schema.$ref")
                        .value("#/components/schemas/ApiProblem"))
                .andExpect(jsonPath("$.paths['/api/v1/offerings/{offeringId}/enrollments/{learnerId}'].delete.responses"
                        + "['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/DropResult"))
                .andExpect(jsonPath("$.components.schemas.ApiProblem.properties.type.format").value("uri"))
                .andExpect(jsonPath("$.components.schemas.ApiProblem.properties.title").exists())
                .andExpect(jsonPath("$.components.schemas.ApiProblem.properties.status.type").value("integer"))
                .andExpect(jsonPath("$.components.schemas.ApiProblem.required",
                        org.hamcrest.Matchers.hasItems("type", "title", "status", "detail", "instance")))
                .andExpect(jsonPath("$.components.schemas.RegistrationView.properties.waitlistPosition.type[1]")
                        .value("null"))
                .andExpect(jsonPath("$.components.schemas.DropResult.properties.promotedLearnerId.type[1]")
                        .value("null"));
    }

    @Test
    void missingAndInvalidIdentifiersReturnSafeBadRequests() throws Exception {
        for (String body : new String[] {
                "{}", "{\"learnerId\":null}", "{\"learnerId\":\"\"}", "{\"learnerId\":\" \"}",
                "{\"learnerId\":\" a\"}", "{\"learnerId\":\"a\\u2003\"}",
                "{\"learnerId\":\"" + "a".repeat(101) + "\"}", "{\"learnerId\":123}",
                "{\"learnerId\":1.5}", "{\"learnerId\":true}", "{\"learnerId\":[]}",
                "{\"learnerId\":{}}"
        }) {
            mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.instance").value(BASE + "/registrations"));
        }
        mvc.perform(get(BASE + "/registrations/{learnerId}", "\u00a0a"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/offerings/{offeringId}/registrations/a", "\u2003CS101-FALL"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/offerings/{offeringId}/registrations/a", "x".repeat(101)))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/registrations/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/api/v1/offerings"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(post(BASE + "/registrations").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }
}
