package com.example.coursewaitlist.api;

import com.example.coursewaitlist.application.DropResult;
import com.example.coursewaitlist.application.EnrollmentOperations;
import com.example.coursewaitlist.application.RegistrationView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/offerings/{offeringId}")
public class RegistrationController {
    private final EnrollmentOperations enrollment;

    public RegistrationController(EnrollmentOperations enrollment) {
        this.enrollment = enrollment;
    }

    @Operation(summary = "Enroll or join the waitlist",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Registration created",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RegistrationView.class))),
                    @ApiResponse(responseCode = "200", description = "Existing active registration",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RegistrationView.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class))),
                    @ApiResponse(responseCode = "404", description = "Offering not found",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class))),
                    @ApiResponse(responseCode = "415", description = "Unsupported content type",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class))),
                    @ApiResponse(responseCode = "422", description = "Prerequisite not satisfied",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class)))
            })
    @PostMapping("/registrations")
    public ResponseEntity<RegistrationView> register(
            @Parameter(description = "Case-sensitive offering ID; 1 to 100 UTF-16 code units, with no leading or trailing whitespace",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100))
            @PathVariable String offeringId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Learner identifier to register",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegistrationRequest.class)))
            @Valid @RequestBody RegistrationRequest request) {
        RegistrationView result = enrollment.register(offeringId, request.learnerId());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result);
    }

    @Operation(summary = "Read the most recent registration",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Registration found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RegistrationView.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid identifier",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class))),
                    @ApiResponse(responseCode = "404", description = "Offering or registration not found",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class)))
            })
    @GetMapping(value = "/registrations/{learnerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public RegistrationView get(
            @Parameter(description = "Case-sensitive offering ID; 1 to 100 UTF-16 code units, with no leading or trailing whitespace",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100))
            @PathVariable String offeringId,
            @Parameter(description = "Case-sensitive learner ID; 1 to 100 UTF-16 code units, with no leading or trailing whitespace",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100))
            @PathVariable String learnerId) {
        return enrollment.getRegistration(offeringId, learnerId);
    }

    @Operation(summary = "Drop an enrollment and promote the first eligible learner",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Enrollment dropped",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DropResult.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid identifier",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class))),
                    @ApiResponse(responseCode = "404", description = "Offering not found",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class))),
                    @ApiResponse(responseCode = "409", description = "Learner is not enrolled",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected server error",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiProblem.class)))
            })
    @DeleteMapping(value = "/enrollments/{learnerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DropResult drop(
            @Parameter(description = "Case-sensitive offering ID; 1 to 100 UTF-16 code units, with no leading or trailing whitespace",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100))
            @PathVariable String offeringId,
            @Parameter(description = "Case-sensitive learner ID; 1 to 100 UTF-16 code units, with no leading or trailing whitespace",
                    required = true, schema = @Schema(minLength = 1, maxLength = 100))
            @PathVariable String learnerId) {
        return enrollment.drop(offeringId, learnerId);
    }
}
