package dev.ayoub.forgewatch.dto;

import jakarta.validation.constraints.NotNull;

public record MeasurementRequest(

        @NotNull(message = "Measurement value is required")
        Double value
) {
}