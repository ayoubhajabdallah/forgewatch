package dev.ayoub.forgewatch.service;

import dev.ayoub.forgewatch.dto.MeasurementResponse;
import dev.ayoub.forgewatch.entity.*;
import dev.ayoub.forgewatch.exception.ResourceNotFoundException;
import dev.ayoub.forgewatch.repository.AlertRepository;
import dev.ayoub.forgewatch.repository.IncidentRepository;
import dev.ayoub.forgewatch.repository.MeasurementRepository;
import dev.ayoub.forgewatch.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final SensorRepository sensorRepository;
    private final MeasurementRepository measurementRepository;
    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;

    @Transactional
    public MeasurementResponse processMeasurement(
            Long sensorId,
            Double value
    ) {

        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sensor not found: " + sensorId
                        ));

        Measurement measurement = new Measurement();
        measurement.setValue(value);
        measurement.setSensor(sensor);

        Measurement savedMeasurement =
                measurementRepository.save(measurement);

        if (value >= sensor.getCriticalThreshold()) {

            Alert alert = createAlert(
                    savedMeasurement,
                    Severity.CRITICAL,
                    "Critical threshold exceeded on sensor "
                            + sensor.getName()
            );

            boolean openIncidentExists =
                    incidentRepository.existsByMachineIdAndStatus(
                            sensor.getMachine().getId(),
                            IncidentStatus.OPEN
                    );

            if (!openIncidentExists) {

                Incident incident = new Incident();
                incident.setSeverity(Severity.CRITICAL);
                incident.setStatus(IncidentStatus.OPEN);
                incident.setDescription(
                        "Critical condition detected by sensor "
                                + sensor.getName()
                );
                incident.setMachine(sensor.getMachine());
                incident.setAlert(alert);

                incidentRepository.save(incident);
            }

        } else if (value >= sensor.getWarningThreshold()) {

            createAlert(
                    savedMeasurement,
                    Severity.WARNING,
                    "Warning threshold exceeded on sensor "
                            + sensor.getName()
            );
        }

        return MeasurementResponse.from(savedMeasurement);
    }

    private Alert createAlert(
            Measurement measurement,
            Severity severity,
            String message
    ) {

        Alert alert = new Alert();
        alert.setMeasurement(measurement);
        alert.setSeverity(severity);
        alert.setMessage(message);

        return alertRepository.save(alert);
    }
}