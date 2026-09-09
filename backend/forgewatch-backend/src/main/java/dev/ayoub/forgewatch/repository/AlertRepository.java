package dev.ayoub.forgewatch.repository;

import dev.ayoub.forgewatch.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findBySeverityOrderByCreatedAtDesc(dev.ayoub.forgewatch.entity.Severity severity);
}