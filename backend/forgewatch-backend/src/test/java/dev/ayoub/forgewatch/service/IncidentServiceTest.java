package dev.ayoub.forgewatch.service;

import dev.ayoub.forgewatch.dto.IncidentResponse;
import dev.ayoub.forgewatch.entity.*;
import dev.ayoub.forgewatch.exception.ResourceNotFoundException;
import dev.ayoub.forgewatch.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private IncidentService incidentService;

    private Incident incident;

    @BeforeEach
    void setUp() {

        Machine machine = new Machine();
        machine.setId(1L);
        machine.setName("Induction Furnace 01");
        machine.setLocation("Production Hall A");
        machine.setStatus(MachineStatus.RUNNING);

        Alert alert = new Alert();
        alert.setId(5L);
        alert.setSeverity(Severity.CRITICAL);
        alert.setMessage("Critical threshold exceeded");

        incident = new Incident();
        incident.setId(10L);
        incident.setSeverity(Severity.CRITICAL);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setDescription("Critical condition detected");
        incident.setMachine(machine);
        incident.setAlert(alert);
    }

    @Test
    void resolvesOpenIncident() {

        when(incidentRepository.findById(10L))
                .thenReturn(Optional.of(incident));

        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IncidentResponse response =
                incidentService.resolveIncident(10L);

        assertEquals(IncidentStatus.RESOLVED, response.status());
        assertNotNull(response.resolvedAt());
        assertEquals(1L, response.machineId());
        assertEquals(5L, response.alertId());

        verify(incidentRepository).save(incident);
    }

    @Test
    void resolvingAlreadyResolvedIncidentFails() {

        incident.setStatus(IncidentStatus.RESOLVED);

        when(incidentRepository.findById(10L))
                .thenReturn(Optional.of(incident));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> incidentService.resolveIncident(10L)
                );

        assertEquals(
                "Incident is already resolved",
                exception.getMessage()
        );

        verify(incidentRepository, never())
                .save(any(Incident.class));
    }

    @Test
    void resolvingMissingIncidentReturnsNotFound() {

        when(incidentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> incidentService.resolveIncident(999L)
        );

        verify(incidentRepository, never())
                .save(any(Incident.class));
    }
}