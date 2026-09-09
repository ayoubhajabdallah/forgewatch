package dev.ayoub.forgewatch.dto;

import dev.ayoub.forgewatch.entity.Measurement;

import java.time.LocalDateTime;

public record MeasurementResponse(
        Long id,
        Double value,
        LocalDateTime timestamp,
        Long sensorId
) {

    public static MeasurementResponse from(Measurement measurement) {
        return new MeasurementResponse(
                measurement.getId(),
                measurement.getValue(),
                measurement.getTimestamp(),
                measurement.getSensor().getId()
        );
    }
}