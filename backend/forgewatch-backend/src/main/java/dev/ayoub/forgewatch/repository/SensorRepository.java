package dev.ayoub.forgewatch.repository;

import dev.ayoub.forgewatch.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    List<Sensor> findByMachineId(Long machineId);
}