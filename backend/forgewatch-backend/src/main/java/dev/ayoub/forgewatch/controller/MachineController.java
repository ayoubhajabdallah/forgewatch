package dev.ayoub.forgewatch.controller;

import dev.ayoub.forgewatch.dto.CreateMachineRequest;
import dev.ayoub.forgewatch.dto.MachineResponse;
import dev.ayoub.forgewatch.service.MachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MachineResponse createMachine(
            @Valid @RequestBody CreateMachineRequest request
    ) {
        return machineService.createMachine(request);
    }

    @GetMapping
    public List<MachineResponse> getAllMachines() {
        return machineService.getAllMachines();
    }

    @GetMapping("/{id}")
    public MachineResponse getMachine(@PathVariable Long id) {
        return machineService.getMachine(id);
    }
}