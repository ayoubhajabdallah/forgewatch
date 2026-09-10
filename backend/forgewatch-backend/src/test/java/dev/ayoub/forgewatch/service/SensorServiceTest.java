package dev.ayoub.forgewatch.service;

import dev.ayoub.forgewatch.dto.CreateSensorRequest;
import dev.ayoub.forgewatch.entity.Machine;
import dev.ayoub.forgewatch.entity.Sensor;
import dev.ayoub.forgewatch.entity.SensorType;
import dev.ayoub.forgewatch.exception.ResourceNotFoundException;
import dev.ayoub.forgewatch.repository.MachineRepository;
import dev.ayoub.forgewatch.repository.SensorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {
    @Mock private SensorRepository sensorRepository;
    @Mock private MachineRepository machineRepository;
    @InjectMocks private SensorService sensorService;

    @ParameterizedTest
    @CsvSource({"70,70", "80,70", "NaN,80", "70,NaN", "-Infinity,80", "70,Infinity",
            ",80", "70,"})
    void rejectsInvalidThresholds(Double warning, Double critical) {
        when(machineRepository.findById(1L)).thenReturn(Optional.of(new Machine()));
        assertThrows(IllegalArgumentException.class,
                () -> sensorService.createSensor(1L, request(warning, critical)));
        verifyNoInteractions(sensorRepository);
    }

    @Test
    void acceptsOrderedNegativeThresholdsAndTrimsText() {
        Machine machine = new Machine();
        machine.setId(1L);
        when(machineRepository.findById(1L)).thenReturn(Optional.of(machine));
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(call -> call.getArgument(0));

        var response = sensorService.createSensor(1L, request(-20.0, -10.0));

        assertEquals("Temperature", response.name());
        assertEquals("C", response.unit());
        assertEquals(1L, response.machineId());
        assertEquals(-20.0, response.warningThreshold());
        assertEquals(-10.0, response.criticalThreshold());
    }

    @Test
    void missingMachineCreatesNoSensor() {
        when(machineRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> sensorService.createSensor(999L, request(70.0, 80.0)));
        verifyNoInteractions(sensorRepository);
    }

    private CreateSensorRequest request(Double warning, Double critical) {
        return new CreateSensorRequest(" Temperature ", SensorType.TEMPERATURE, " C ", warning, critical);
    }
}
