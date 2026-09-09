package dev.ayoub.forgewatch.dto;

import dev.ayoub.forgewatch.entity.Incident;
import dev.ayoub.forgewatch.entity.IncidentStatus;
import dev.ayoub.forgewatch.entity.Severity;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        Severity severity,
        IncidentStatus status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        Long machineId,
        String machineName,
        Long alertId
) {

    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getDescription(),
                incident.getCreatedAt(),
                incident.getResolvedAt(),
                incident.getMachine().getId(),
                incident.getMachine().getName(),
                incident.getAlert().getId()
        );
    }
}