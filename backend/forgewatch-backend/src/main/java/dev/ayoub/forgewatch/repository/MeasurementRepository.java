package dev.ayoub.forgewatch.repository;

import dev.ayoub.forgewatch.entity.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findBySensorIdOrderByTimestampDesc(Long sensorId);
}