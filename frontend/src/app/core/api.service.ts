import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { timeout } from 'rxjs';
import { CreateMachine, CreateSensor, Incident, Machine, Measurement, Sensor } from './api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  machines() {
    return this.http.get<Machine[]>('/api/machines').pipe(timeout(10000));
  }
  incidents(open = false) {
    return this.http
      .get<Incident[]>(open ? '/api/incidents/open' : '/api/incidents')
      .pipe(timeout(10000));
  }
  sensors(machineId: number) {
    return this.http.get<Sensor[]>(`/api/machines/${machineId}/sensors`).pipe(timeout(10000));
  }
  createMachine(body: CreateMachine) {
    return this.http.post<Machine>('/api/machines', body).pipe(timeout(10000));
  }
  createSensor(machineId: number, body: CreateSensor) {
    return this.http.post<Sensor>(`/api/machines/${machineId}/sensors`, body).pipe(timeout(10000));
  }
  measure(sensorId: number, value: number) {
    return this.http
      .post<Measurement>(`/api/sensors/${sensorId}/measurements`, { value })
      .pipe(timeout(10000));
  }
  resolve(id: number) {
    return this.http.patch<Incident>(`/api/incidents/${id}/resolve`, {}).pipe(timeout(10000));
  }
}
export function apiError(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status >= 400 && error.status < 500) {
    return typeof error.error?.message === 'string'
      ? error.error.message
      : 'The request could not be completed.';
  }
  return 'Unable to reach ForgeWatch. Check the backend connection and try again. If you submitted data, refresh before retrying.';
}
