package dev.ayoub.forgewatch.dto;

import dev.ayoub.forgewatch.entity.Machine;
import dev.ayoub.forgewatch.entity.MachineStatus;

import java.time.LocalDateTime;

public record MachineResponse(
        Long id,
        String name,
        String location,
        MachineStatus status,
        LocalDateTime createdAt
) {

    public static MachineResponse from(Machine machine) {
        return new MachineResponse(
                machine.getId(),
                machine.getName(),
                machine.getLocation(),
                machine.getStatus(),
                machine.getCreatedAt()
        );
    }
}