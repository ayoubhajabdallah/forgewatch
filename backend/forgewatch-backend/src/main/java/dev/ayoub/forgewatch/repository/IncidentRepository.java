package dev.ayoub.forgewatch.repository;

import dev.ayoub.forgewatch.entity.Incident;
import dev.ayoub.forgewatch.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);

    boolean existsByMachineIdAndStatus(Long machineId, IncidentStatus status);
}