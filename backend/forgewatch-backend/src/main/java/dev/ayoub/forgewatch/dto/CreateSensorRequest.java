package dev.ayoub.forgewatch.dto;

import dev.ayoub.forgewatch.entity.SensorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSensorRequest(

        @NotBlank(message = "Sensor name is required")
        @Size(max = 100)
        String name,

        @NotNull(message = "Sensor type is required")
        SensorType type,

        @NotBlank(message = "Sensor unit is required")
        @Size(max = 20)
        String unit,

        @NotNull(message = "Warning threshold is required")
        Double warningThreshold,

        @NotNull(message = "Critical threshold is required")
        Double criticalThreshold
) {
}
