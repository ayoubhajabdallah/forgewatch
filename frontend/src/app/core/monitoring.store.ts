import { computed, inject, Injectable, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ApiService, apiError } from './api.service';
import { Incident, Machine } from './api.models';

@Injectable({ providedIn: 'root' })
export class MonitoringStore {
  private readonly api = inject(ApiService);
  private revision = 0;
  readonly machines = signal<Machine[]>([]);
  readonly incidents = signal<Incident[]>([]);
  readonly openIncidents = signal<Incident[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly lastUpdated = signal<Date | null>(null);
  readonly resolving = signal<Set<number>>(new Set());
  readonly critical = computed(
    () => this.openIncidents().filter((i) => i.severity === 'CRITICAL').length,
  );
  readonly recent = computed(() =>
    [...this.incidents()].sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id),
  );
  refresh() {
    const revision = ++this.revision;
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      machines: this.api.machines(),
      incidents: this.api.incidents(),
      open: this.api.incidents(true),
    }).subscribe({
      next: (data) => {
        if (revision !== this.revision) return;
        this.machines.set(data.machines);
        this.incidents.set(data.incidents);
        this.openIncidents.set(data.open);
        this.lastUpdated.set(new Date());
        this.loading.set(false);
      },
      error: (error) => {
        if (revision !== this.revision) return;
        this.error.set(apiError(error));
        this.loading.set(false);
      },
    });
  }
  resolve(id: number) {
    if (this.resolving().has(id)) return;
    this.resolving.update((ids) => new Set([...ids, id]));
    this.error.set('');
    this.notice.set('');
    this.api.resolve(id).subscribe({
      next: () => {
        this.release(id);
        this.notice.set(`Incident #${id} resolved.`);
        this.refresh();
      },
      error: (error) => {
        this.release(id);
        this.error.set(apiError(error));
      },
    });
  }
  private release(id: number) {
    this.resolving.update((ids) => {
      const next = new Set(ids);
      next.delete(id);
      return next;
    });
  }
}
