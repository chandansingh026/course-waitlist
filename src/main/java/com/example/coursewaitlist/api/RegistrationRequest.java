package com.example.coursewaitlist.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

public record RegistrationRequest(@Schema(description = "Case-sensitive learner ID; 1 to 100 UTF-16 code units, with no leading or trailing whitespace",
                                          example = "learner-1001", minLength = 1, maxLength = 100)
                                  @NotBlank @Size(min = 1, max = 100)
                                  @JsonDeserialize(using = IdentifierDeserializer.class) String learnerId) {
}
