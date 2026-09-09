package dev.ayoub.forgewatch.dto;

import dev.ayoub.forgewatch.entity.Sensor;
import dev.ayoub.forgewatch.entity.SensorType;

public record SensorResponse(
        Long id,
        String name,
        SensorType type,
        String unit,
        Double warningThreshold,
        Double criticalThreshold,
        Long machineId
) {

    public static SensorResponse from(Sensor sensor) {
        return new SensorResponse(
                sensor.getId(),
                sensor.getName(),
                sensor.getType(),
                sensor.getUnit(),
                sensor.getWarningThreshold(),
                sensor.getCriticalThreshold(),
                sensor.getMachine().getId()
        );
    }
}