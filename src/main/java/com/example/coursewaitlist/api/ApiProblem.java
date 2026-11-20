package com.example.coursewaitlist.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiProblem", description = "Problem response returned by the API")
public record ApiProblem(
        @Schema(description = "Problem type identifier", format = "uri",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String type,
        @Schema(description = "Short, human-readable summary", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,
        @Schema(description = "HTTP status code", requiredMode = Schema.RequiredMode.REQUIRED)
        int status,
        @Schema(description = "Human-readable explanation", requiredMode = Schema.RequiredMode.REQUIRED)
        String detail,
        @Schema(description = "Request URI reference that caused the problem", format = "uri-reference",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String instance) {
}
