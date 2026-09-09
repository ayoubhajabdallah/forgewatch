package dev.ayoub.forgewatch.repository;

import dev.ayoub.forgewatch.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, Long> {
}