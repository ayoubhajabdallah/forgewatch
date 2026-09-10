package dev.ayoub.forgewatch.service;

import dev.ayoub.forgewatch.dto.MeasurementResponse;
import dev.ayoub.forgewatch.entity.*;
import dev.ayoub.forgewatch.exception.ResourceNotFoundException;
import dev.ayoub.forgewatch.repository.AlertRepository;
import dev.ayoub.forgewatch.repository.IncidentRepository;
import dev.ayoub.forgewatch.repository.MeasurementRepository;
import dev.ayoub.forgewatch.repository.MachineRepository;
import dev.ayoub.forgewatch.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    @Mock
    private MachineRepository machineRepository;

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

    }

    private void stubMeasurementSave() {
        when(sensorRepository.findById(10L))
                .thenReturn(Optional.of(sensor));

        when(measurementRepository.save(any(Measurement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void normalMeasurementCreatesNoAlertOrIncident() {
        stubMeasurementSave();

        MeasurementResponse response =
                monitoringService.processMeasurement(10L, 60.0);

        assertEquals(60.0, response.value());
        assertEquals(10L, response.sensorId());

        verifyNoInteractions(alertRepository);
        verifyNoInteractions(incidentRepository);
        verifyNoInteractions(machineRepository);
    }

    @ParameterizedTest
    @ValueSource(doubles = {70.0, 75.0})
    void warningMeasurementCreatesWarningAlertOnly(double value) {
        stubMeasurementSave();

        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        monitoringService.processMeasurement(10L, value);

        ArgumentCaptor<Alert> alertCaptor =
                ArgumentCaptor.forClass(Alert.class);

        verify(alertRepository).save(alertCaptor.capture());

        Alert alert = alertCaptor.getValue();

        assertEquals(Severity.WARNING, alert.getSeverity());
        assertEquals(value, alert.getMeasurement().getValue());

        verifyNoInteractions(incidentRepository);
        verifyNoInteractions(machineRepository);
    }

    @ParameterizedTest
    @ValueSource(doubles = {80.0, 85.0})
    void criticalMeasurementCreatesCriticalAlertAndIncident(double value) {
        stubMeasurementSave();
        when(machineRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sensor.getMachine()));

        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(
                incidentRepository.existsByMachineIdAndStatus(
                        1L,
                        IncidentStatus.OPEN
                )
        ).thenReturn(false);

        monitoringService.processMeasurement(10L, value);

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
        assertSame(alertCaptor.getValue(), incident.getAlert());

        var order = inOrder(machineRepository, incidentRepository);
        order.verify(machineRepository).findByIdForUpdate(1L);
        order.verify(incidentRepository).existsByMachineIdAndStatus(1L, IncidentStatus.OPEN);
    }

    @Test
    void criticalMeasurementDoesNotCreateDuplicateOpenIncident() {
        stubMeasurementSave();
        when(machineRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sensor.getMachine()));

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

    @ParameterizedTest
    @NullSource
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteOrMissingValuesBeforeWriting(Double value) {
        assertThrows(IllegalArgumentException.class,
                () -> monitoringService.processMeasurement(10L, value));
        verifyNoInteractions(sensorRepository, measurementRepository, alertRepository,
                incidentRepository, machineRepository);
    }

    @Test
    void missingSensorCreatesNothing() {
        when(sensorRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> monitoringService.processMeasurement(999L, 85.0));
        verifyNoInteractions(measurementRepository, alertRepository, incidentRepository, machineRepository);
    }
}
