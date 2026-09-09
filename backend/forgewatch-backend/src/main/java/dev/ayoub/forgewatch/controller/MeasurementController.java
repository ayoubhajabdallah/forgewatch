package dev.ayoub.forgewatch.controller;

import dev.ayoub.forgewatch.dto.MeasurementRequest;
import dev.ayoub.forgewatch.dto.MeasurementResponse;
import dev.ayoub.forgewatch.service.MonitoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sensors/{sensorId}/measurements")
@RequiredArgsConstructor
public class MeasurementController {

    private final MonitoringService monitoringService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeasurementResponse createMeasurement(
            @PathVariable Long sensorId,
            @Valid @RequestBody MeasurementRequest request
    ) {
        return monitoringService.processMeasurement(
                sensorId,
                request.value()
        );
    }
}