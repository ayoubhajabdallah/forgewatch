package dev.ayoub.forgewatch;

import dev.ayoub.forgewatch.dto.CreateMachineRequest;
import dev.ayoub.forgewatch.dto.CreateSensorRequest;
import dev.ayoub.forgewatch.dto.IncidentResponse;
import dev.ayoub.forgewatch.entity.Incident;
import dev.ayoub.forgewatch.entity.IncidentStatus;
import dev.ayoub.forgewatch.entity.SensorType;
import dev.ayoub.forgewatch.entity.Severity;
import dev.ayoub.forgewatch.repository.*;
import dev.ayoub.forgewatch.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.jpa.show-sql=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ForgewatchBackendApplicationTests {
    // A unique schema prevents the full-context tests from modifying development tables.
    private static final String TEST_SCHEMA = "forgewatch_test_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void testDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv().getOrDefault(
                "FORGEWATCH_TEST_DATABASE_URL", "jdbc:postgresql://localhost:5432/forgewatch"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault(
                "FORGEWATCH_TEST_DATABASE_USERNAME", "forgewatch"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault(
                "FORGEWATCH_TEST_DATABASE_PASSWORD", "forgewatch"));
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> TEST_SCHEMA);
    }

    @Autowired private MachineService machineService;
    @Autowired private SensorService sensorService;
    @Autowired private MonitoringService monitoringService;
    @Autowired private IncidentService incidentService;
    @Autowired private MachineRepository machineRepository;
    @Autowired private SensorRepository sensorRepository;
    @Autowired private MeasurementRepository measurementRepository;
    @Autowired private AlertRepository alertRepository;
    @MockitoSpyBean private IncidentRepository incidentRepository;

    @BeforeEach
    void clearTestSchema() {
        incidentRepository.deleteAllInBatch();
        alertRepository.deleteAllInBatch();
        measurementRepository.deleteAllInBatch();
        sensorRepository.deleteAllInBatch();
        machineRepository.deleteAllInBatch();
    }

    @Test
    void persistsThresholdFlowAndAllowsNewIncidentAfterResolution() {
        var machine = machineService.createMachine(new CreateMachineRequest("Furnace", "Hall A"));
        Long sensorId = createSensor(machine.id());

        var normal = monitoringService.processMeasurement(sensorId, Math.nextDown(70.0));
        assertNotNull(normal.id());
        assertNotNull(normal.timestamp());
        assertEquals(0, alertRepository.count());

        monitoringService.processMeasurement(sensorId, 70.0);
        assertEquals(1, alertRepository.findBySeverityOrderByCreatedAtDesc(Severity.WARNING).size());
        assertEquals(0, incidentRepository.count());

        monitoringService.processMeasurement(sensorId, 80.0);
        monitoringService.processMeasurement(sensorId, 85.0);
        assertEquals(4, measurementRepository.count());
        assertEquals(2, alertRepository.findBySeverityOrderByCreatedAtDesc(Severity.CRITICAL).size());
        assertEquals(1, incidentRepository.count());

        var open = incidentService.getOpenIncidents().getFirst();
        assertEquals(machine.id(), open.machineId());
        assertEquals("Furnace", open.machineName());
        assertNotNull(open.alertId());
        assertNotNull(open.createdAt());
        assertEquals(IncidentStatus.RESOLVED, incidentService.resolveIncident(open.id()).status());
        var resolved = incidentService.getIncident(open.id());
        assertNotNull(resolved.resolvedAt());
        assertTrue(incidentService.getOpenIncidents().isEmpty());

        monitoringService.processMeasurement(sensorId, 80.0);
        assertEquals(2, incidentService.getAllIncidents().size());
        assertEquals(1, incidentService.getOpenIncidents().size());
        assertNotEquals(open.id(), incidentService.getOpenIncidents().getFirst().id());
    }

    @Test
    void incidentFailureRollsBackMeasurementAndAlert() {
        Long machineId = machineService.createMachine(new CreateMachineRequest("Furnace", "Hall A")).id();
        Long sensorId = createSensor(machineId);
        doThrow(new IllegalStateException("Simulated incident persistence failure"))
                .when(incidentRepository).save(any(Incident.class));

        assertThrows(IllegalStateException.class, () -> monitoringService.processMeasurement(sensorId, 80.0));

        assertEquals(0, measurementRepository.count());
        assertEquals(0, alertRepository.count());
        assertEquals(0, incidentRepository.count());
    }

    @Test
    void concurrentCriticalMeasurementsAcrossSensorsCreateOneOpenIncident() throws Exception {
        Long machineId = machineService.createMachine(new CreateMachineRequest("Furnace", "Hall A")).id();
        Long firstSensor = createSensor(machineId);
        Long secondSensor = createSensor(machineId);
        List<Callable<Long>> requests = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Long sensorId = i % 2 == 0 ? firstSensor : secondSensor;
            requests.add(() -> monitoringService.processMeasurement(sensorId, 80.0).id());
        }

        assertEquals(6, concurrently(requests).stream().distinct().count());
        assertEquals(6, measurementRepository.count());
        assertEquals(6, alertRepository.count());
        assertEquals(1, incidentService.getOpenIncidents().size());

        Long otherMachine = machineService.createMachine(new CreateMachineRequest("Other furnace", "Hall B")).id();
        monitoringService.processMeasurement(createSensor(otherMachine), 80.0);
        assertEquals(2, incidentService.getOpenIncidents().size());
    }

    @Test
    void concurrentResolutionSucceedsOnceAndPreservesResolutionTime() throws Exception {
        Long machineId = machineService.createMachine(new CreateMachineRequest("Furnace", "Hall A")).id();
        monitoringService.processMeasurement(createSensor(machineId), 80.0);
        Long incidentId = incidentService.getOpenIncidents().getFirst().id();
        Callable<IncidentResponse> resolve = () -> {
            try {
                return incidentService.resolveIncident(incidentId);
            } catch (IllegalArgumentException exception) {
                assertEquals("Incident is already resolved", exception.getMessage());
                return null;
            }
        };

        var results = concurrently(List.of(resolve, resolve));
        var successes = results.stream().filter(java.util.Objects::nonNull).toList();
        assertEquals(1, successes.size());
        var persisted = incidentService.getIncident(incidentId);
        assertEquals(IncidentStatus.RESOLVED, persisted.status());
        // PostgreSQL timestamps have microsecond precision.
        assertTrue(java.time.Duration.between(successes.getFirst().resolvedAt(), persisted.resolvedAt())
                .abs().toNanos() < 1_000);
    }

    private Long createSensor(Long machineId) {
        return sensorService.createSensor(machineId,
                new CreateSensorRequest("Temperature", SensorType.TEMPERATURE, "C", 70.0, 80.0)).id();
    }

    private <T> List<T> concurrently(List<Callable<T>> requests) throws Exception {
        var executor = Executors.newFixedThreadPool(requests.size());
        var start = new CyclicBarrier(requests.size());
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (var request : requests) {
                futures.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return request.call();
                }));
            }
            List<T> results = new ArrayList<>();
            for (var future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
