package dev.ayoub.forgewatch.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateMachineRequest(

        @NotBlank(message = "Machine name is required")
        String name,

        @NotBlank(message = "Machine location is required")
        String location
) {
}