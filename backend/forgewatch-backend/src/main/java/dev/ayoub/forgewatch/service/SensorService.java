package dev.ayoub.forgewatch.service;

import dev.ayoub.forgewatch.dto.CreateSensorRequest;
import dev.ayoub.forgewatch.dto.SensorResponse;
import dev.ayoub.forgewatch.entity.Machine;
import dev.ayoub.forgewatch.entity.Sensor;
import dev.ayoub.forgewatch.exception.ResourceNotFoundException;
import dev.ayoub.forgewatch.repository.MachineRepository;
import dev.ayoub.forgewatch.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorRepository sensorRepository;
    private final MachineRepository machineRepository;

    @Transactional
    public SensorResponse createSensor(
            Long machineId,
            CreateSensorRequest request
    ) {

        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Machine not found: " + machineId
                        ));

        if (request.warningThreshold() == null || !Double.isFinite(request.warningThreshold())
                || request.criticalThreshold() == null || !Double.isFinite(request.criticalThreshold())) {
            throw new IllegalArgumentException("Thresholds must be finite numbers");
        }

        if (request.criticalThreshold() <= request.warningThreshold()) {
            throw new IllegalArgumentException(
                    "Critical threshold must be greater than warning threshold"
            );
        }

        Sensor sensor = new Sensor();
        sensor.setName(request.name().trim());
        sensor.setType(request.type());
        sensor.setUnit(request.unit().trim());
        sensor.setWarningThreshold(request.warningThreshold());
        sensor.setCriticalThreshold(request.criticalThreshold());
        sensor.setMachine(machine);

        return SensorResponse.from(sensorRepository.save(sensor));
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> getSensorsByMachine(Long machineId) {

        if (!machineRepository.existsById(machineId)) {
            throw new ResourceNotFoundException(
                    "Machine not found: " + machineId
            );
        }

        return sensorRepository.findByMachineId(machineId)
                .stream()
                .map(SensorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SensorResponse getSensor(Long id) {

        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sensor not found: " + id
                        ));

        return SensorResponse.from(sensor);
    }
}
