package dev.ayoub.forgewatch.service;

import dev.ayoub.forgewatch.dto.IncidentResponse;
import dev.ayoub.forgewatch.entity.Incident;
import dev.ayoub.forgewatch.entity.IncidentStatus;
import dev.ayoub.forgewatch.exception.ResourceNotFoundException;
import dev.ayoub.forgewatch.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;

    @Transactional(readOnly = true)
    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll()
                .stream()
                .map(IncidentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getOpenIncidents() {
        return incidentRepository
                .findByStatusOrderByCreatedAtDesc(IncidentStatus.OPEN)
                .stream()
                .map(IncidentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(Long id) {

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Incident not found: " + id
                        ));

        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse resolveIncident(Long id) {

        Incident incident = incidentRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Incident not found: " + id
                        ));

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new IllegalArgumentException(
                    "Incident is already resolved"
            );
        }

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());

        return IncidentResponse.from(
                incidentRepository.save(incident)
        );
    }
}
