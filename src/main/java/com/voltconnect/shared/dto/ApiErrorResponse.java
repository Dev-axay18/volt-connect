package com.voltconnect.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error response body returned by {@link com.voltconnect.shared.exceptions.GlobalExceptionHandler}
 * for all error conditions.
 *
 * <p>Example JSON:
 * <pre>
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Charger not found with id: abc-123",
 *   "path": "/api/v1/chargers/abc-123",
 *   "timestamp": "2024-01-15T10:30:00Z",
 *   "fieldErrors": null
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {

    /**
     * Represents a single field-level validation error.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldError(
            String field,
            String rejectedValue,
            String message
    ) {}

    // ── Convenience factory methods ──────────────────────────────────────────

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(status, error, message, path, Instant.now(), null);
    }

    public static ApiErrorResponse of(int status, String error, String message, String path,
                                      List<FieldError> fieldErrors) {
        return new ApiErrorResponse(status, error, message, path, Instant.now(), fieldErrors);
    }
}
