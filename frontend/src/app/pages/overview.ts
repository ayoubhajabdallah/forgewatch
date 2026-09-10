import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MonitoringStore } from '../core/monitoring.store';
import { Badge } from '../shared/badge';
import { IncidentTable } from '../shared/incident-table';
import { SensorPanel } from '../shared/sensor-panel';
@Component({
  standalone: true,
  imports: [RouterLink, Badge, IncidentTable, SensorPanel],
  templateUrl: './overview.html',
})
export class Overview {
  readonly store = inject(MonitoringStore);
  readonly selectedId = signal<number | null>(null);
  readonly selected = computed(
    () => this.store.machines().find((m) => m.id === this.selectedId()) ?? this.store.machines()[0],
  );
  readonly running = computed(
    () => this.store.machines().filter((m) => m.status === 'RUNNING').length,
  );
  select(value: string) {
    this.selectedId.set(Number(value));
  }
}
