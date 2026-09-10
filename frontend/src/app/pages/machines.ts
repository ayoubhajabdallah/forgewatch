import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MonitoringStore } from '../core/monitoring.store';
import { Badge } from '../shared/badge';
import { CreateMachineForm } from '../shared/create-machine';
import { SensorPanel } from '../shared/sensor-panel';
@Component({
  standalone: true,
  imports: [RouterLink, Badge, CreateMachineForm, SensorPanel],
  templateUrl: './machines.html',
})
export class Machines {
  readonly store = inject(MonitoringStore);
  readonly router = inject(Router);
  private readonly params = toSignal(inject(ActivatedRoute).paramMap);
  readonly query = signal('');
  readonly filtered = computed(() =>
    this.store
      .machines()
      .filter((m) =>
        (m.name + ' ' + m.location).toLowerCase().includes(this.query().toLowerCase().trim()),
      ),
  );
  readonly selected = computed(() => {
    const id = this.params()?.get('id');
    return id ? this.store.machines().find((m) => m.id === Number(id)) : this.store.machines()[0];
  });
  readonly missing = computed(
    () => !!this.params()?.get('id') && !this.selected() && !this.store.loading(),
  );
}
