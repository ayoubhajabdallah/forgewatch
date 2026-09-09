package dev.ayoub.forgewatch.service;

import dev.ayoub.forgewatch.dto.CreateMachineRequest;
import dev.ayoub.forgewatch.dto.MachineResponse;
import dev.ayoub.forgewatch.entity.Machine;
import dev.ayoub.forgewatch.entity.MachineStatus;
import dev.ayoub.forgewatch.exception.ResourceNotFoundException;
import dev.ayoub.forgewatch.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineRepository;

    @Transactional
    public MachineResponse createMachine(CreateMachineRequest request) {

        Machine machine = new Machine();
        machine.setName(request.name().trim());
        machine.setLocation(request.location().trim());
        machine.setStatus(MachineStatus.IDLE);

        return MachineResponse.from(machineRepository.save(machine));
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> getAllMachines() {
        return machineRepository.findAll()
                .stream()
                .map(MachineResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MachineResponse getMachine(Long id) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Machine not found: " + id
                        ));

        return MachineResponse.from(machine);
    }
}