package dev.ayoub.forgewatch.controller;

import dev.ayoub.forgewatch.dto.CreateSensorRequest;
import dev.ayoub.forgewatch.dto.SensorResponse;
import dev.ayoub.forgewatch.service.SensorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;

    @PostMapping("/api/machines/{machineId}/sensors")
    @ResponseStatus(HttpStatus.CREATED)
    public SensorResponse createSensor(
            @PathVariable Long machineId,
            @Valid @RequestBody CreateSensorRequest request
    ) {
        return sensorService.createSensor(machineId, request);
    }

    @GetMapping("/api/machines/{machineId}/sensors")
    public List<SensorResponse> getSensorsByMachine(
            @PathVariable Long machineId
    ) {
        return sensorService.getSensorsByMachine(machineId);
    }

    @GetMapping("/api/sensors/{id}")
    public SensorResponse getSensor(@PathVariable Long id) {
        return sensorService.getSensor(id);
    }
}