package dev.ayoub.forgewatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMachineRequest(

        @NotBlank(message = "Machine name is required")
        @Size(max = 100)
        String name,

        @NotBlank(message = "Machine location is required")
        @Size(max = 150)
        String location
) {
}
