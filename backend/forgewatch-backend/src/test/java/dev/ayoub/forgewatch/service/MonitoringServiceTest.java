package dev.ayoub.forgewatch.service;

import dev.ayoub.forgewatch.dto.MeasurementResponse;
import dev.ayoub.forgewatch.entity.*;
import dev.ayoub.forgewatch.repository.AlertRepository;
import dev.ayoub.forgewatch.repository.IncidentRepository;
import dev.ayoub.forgewatch.repository.MeasurementRepository;
import dev.ayoub.forgewatch.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private MeasurementRepository measurementRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private MonitoringService monitoringService;

    private Sensor sensor;

    @BeforeEach
    void setUp() {

        Machine machine = new Machine();
        machine.setId(1L);
        machine.setName("Induction Furnace 01");
        machine.setLocation("Production Hall A");
        machine.setStatus(MachineStatus.RUNNING);

        sensor = new Sensor();
        sensor.setId(10L);
        sensor.setName("Melt Temperature");
        sensor.setType(SensorType.TEMPERATURE);
        sensor.setUnit("C");
        sensor.setWarningThreshold(70.0);
        sensor.setCriticalThreshold(80.0);
        sensor.setMachine(machine);

        when(sensorRepository.findById(10L))
                .thenReturn(Optional.of(sensor));

        when(measurementRepository.save(any(Measurement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void normalMeasurementCreatesNoAlertOrIncident() {

        MeasurementResponse response =
                monitoringService.processMeasurement(10L, 60.0);

        assertEquals(60.0, response.value());
        assertEquals(10L, response.sensorId());

        verifyNoInteractions(alertRepository);
        verifyNoInteractions(incidentRepository);
    }

    @Test
    void warningMeasurementCreatesWarningAlertOnly() {

        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        monitoringService.processMeasurement(10L, 75.0);

        ArgumentCaptor<Alert> alertCaptor =
                ArgumentCaptor.forClass(Alert.class);

        verify(alertRepository).save(alertCaptor.capture());

        Alert alert = alertCaptor.getValue();

        assertEquals(Severity.WARNING, alert.getSeverity());
        assertEquals(75.0, alert.getMeasurement().getValue());

        verifyNoInteractions(incidentRepository);
    }

    @Test
    void criticalMeasurementCreatesCriticalAlertAndIncident() {

        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(
                incidentRepository.existsByMachineIdAndStatus(
                        1L,
                        IncidentStatus.OPEN
                )
        ).thenReturn(false);

        monitoringService.processMeasurement(10L, 85.0);

        ArgumentCaptor<Alert> alertCaptor =
                ArgumentCaptor.forClass(Alert.class);

        verify(alertRepository).save(alertCaptor.capture());

        assertEquals(
                Severity.CRITICAL,
                alertCaptor.getValue().getSeverity()
        );

        ArgumentCaptor<Incident> incidentCaptor =
                ArgumentCaptor.forClass(Incident.class);

        verify(incidentRepository).save(incidentCaptor.capture());

        Incident incident = incidentCaptor.getValue();

        assertEquals(Severity.CRITICAL, incident.getSeverity());
        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(1L, incident.getMachine().getId());
    }

    @Test
    void criticalMeasurementDoesNotCreateDuplicateOpenIncident() {

        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(
                incidentRepository.existsByMachineIdAndStatus(
                        1L,
                        IncidentStatus.OPEN
                )
        ).thenReturn(true);

        monitoringService.processMeasurement(10L, 90.0);

        verify(alertRepository, times(1))
                .save(any(Alert.class));

        verify(incidentRepository, never())
                .save(any(Incident.class));
    }
}