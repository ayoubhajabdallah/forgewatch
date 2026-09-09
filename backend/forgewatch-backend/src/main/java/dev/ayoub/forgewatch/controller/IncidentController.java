package dev.ayoub.forgewatch.controller;

import dev.ayoub.forgewatch.dto.IncidentResponse;
import dev.ayoub.forgewatch.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public List<IncidentResponse> getAllIncidents() {
        return incidentService.getAllIncidents();
    }

    @GetMapping("/open")
    public List<IncidentResponse> getOpenIncidents() {
        return incidentService.getOpenIncidents();
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncident(@PathVariable Long id) {
        return incidentService.getIncident(id);
    }

    @PatchMapping("/{id}/resolve")
    public IncidentResponse resolveIncident(@PathVariable Long id) {
        return incidentService.resolveIncident(id);
    }
}