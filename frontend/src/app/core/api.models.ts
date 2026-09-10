export type MachineStatus = 'RUNNING' | 'IDLE' | 'MAINTENANCE' | 'OFFLINE';
export type SensorType = 'TEMPERATURE' | 'PRESSURE' | 'VIBRATION' | 'POWER' | 'COOLING_FLOW';
export type Severity = 'WARNING' | 'CRITICAL';
export type IncidentStatus = 'OPEN' | 'RESOLVED';
export interface Machine {
  id: number;
  name: string;
  location: string;
  status: MachineStatus;
  createdAt: string;
}
export interface Sensor {
  id: number;
  name: string;
  type: SensorType;
  unit: string;
  warningThreshold: number;
  criticalThreshold: number;
  machineId: number;
}
export interface Incident {
  id: number;
  severity: Severity;
  status: IncidentStatus;
  description: string;
  createdAt: string;
  resolvedAt: string | null;
  machineId: number;
  machineName: string;
  alertId: number;
}
export interface Measurement {
  id: number;
  value: number;
  timestamp: string;
  sensorId: number;
}
export interface CreateMachine {
  name: string;
  location: string;
}
export interface CreateSensor {
  name: string;
  type: SensorType;
  unit: string;
  warningThreshold: number;
  criticalThreshold: number;
}
