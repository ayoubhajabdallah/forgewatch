package dev.ayoub.forgewatch.controller;

import dev.ayoub.forgewatch.dto.*;
import dev.ayoub.forgewatch.entity.IncidentStatus;
import dev.ayoub.forgewatch.entity.MachineStatus;
import dev.ayoub.forgewatch.entity.Severity;
import dev.ayoub.forgewatch.exception.GlobalExceptionHandler;
import dev.ayoub.forgewatch.exception.ResourceNotFoundException;
import dev.ayoub.forgewatch.service.IncidentService;
import dev.ayoub.forgewatch.service.MachineService;
import dev.ayoub.forgewatch.service.MonitoringService;
import dev.ayoub.forgewatch.service.SensorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ApiControllerTest {

    @Mock
    private MachineService machineService;

    @Mock
    private SensorService sensorService;

    @Mock
    private MonitoringService monitoringService;

    @Mock
    private IncidentService incidentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        MachineController machineController =
                new MachineController(machineService);

        SensorController sensorController =
                new SensorController(sensorService);

        MeasurementController measurementController =
                new MeasurementController(monitoringService);

        IncidentController incidentController =
                new IncidentController(incidentService);

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        machineController,
                        sensorController,
                        measurementController,
                        incidentController
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createsMachine() throws Exception {

        MachineResponse response =
                new MachineResponse(
                        1L,
                        "Induction Furnace 01",
                        "Production Hall A",
                        MachineStatus.IDLE,
                        LocalDateTime.now()
                );

        when(machineService.createMachine(any(CreateMachineRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/machines")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Induction Furnace 01",
                                          "location": "Production Hall A"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Induction Furnace 01"))
                .andExpect(jsonPath("$.status")
                        .value("IDLE"));
    }

    @Test
    void rejectsInvalidMachineRequest() throws Exception {

        mockMvc.perform(
                        post("/api/machines")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "",
                                          "location": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returns404ForMissingMachine() throws Exception {

        when(machineService.getMachine(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Machine not found: 999"
                        )
                );

        mockMvc.perform(
                        get("/api/machines/999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Machine not found: 999"));
    }

    @Test
    void createsMeasurement() throws Exception {

        MeasurementResponse response =
                new MeasurementResponse(
                        5L,
                        85.0,
                        LocalDateTime.now(),
                        10L
                );

        when(monitoringService.processMeasurement(
                eq(10L),
                eq(85.0)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/sensors/10/measurements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "value": 85
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.value").value(85.0))
                .andExpect(jsonPath("$.sensorId").value(10));
    }

    @Test
    void returnsOpenIncidents() throws Exception {

        IncidentResponse incident =
                new IncidentResponse(
                        7L,
                        Severity.CRITICAL,
                        IncidentStatus.OPEN,
                        "Critical condition detected",
                        LocalDateTime.now(),
                        null,
                        1L,
                        "Induction Furnace 01",
                        4L
                );

        when(incidentService.getOpenIncidents())
                .thenReturn(List.of(incident));

        mockMvc.perform(
                        get("/api/incidents/open")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].severity")
                        .value("CRITICAL"))
                .andExpect(jsonPath("$[0].status")
                        .value("OPEN"));
    }

    @Test
    void resolvesIncident() throws Exception {

        IncidentResponse resolved =
                new IncidentResponse(
                        7L,
                        Severity.CRITICAL,
                        IncidentStatus.RESOLVED,
                        "Critical condition detected",
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        1L,
                        "Induction Furnace 01",
                        4L
                );

        when(incidentService.resolveIncident(7L))
                .thenReturn(resolved);

        mockMvc.perform(
                        patch("/api/incidents/7/resolve")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status")
                        .value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists());
    }
}