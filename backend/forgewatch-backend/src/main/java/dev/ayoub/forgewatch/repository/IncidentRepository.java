package dev.ayoub.forgewatch.repository;

import dev.ayoub.forgewatch.entity.Incident;
import dev.ayoub.forgewatch.entity.IncidentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Override
    @EntityGraph(attributePaths = "machine")
    List<Incident> findAll();

    @EntityGraph(attributePaths = "machine")
    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);

    boolean existsByMachineIdAndStatus(Long machineId, IncidentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incident i where i.id = :id")
    Optional<Incident> findByIdForUpdate(@Param("id") Long id);
}
