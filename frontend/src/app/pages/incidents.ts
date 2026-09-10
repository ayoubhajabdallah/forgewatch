import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MonitoringStore } from '../core/monitoring.store';
import { IncidentTable } from '../shared/incident-table';
@Component({
  standalone: true,
  imports: [RouterLink, IncidentTable],
  template: `
    <div class="page-heading">
      <div>
        <h1>Incidents</h1>
        <p>Review critical conditions and close the loop on your response.</p>
      </div>
      <span class="workspace-tag"
        >{{ store.lastUpdated() ? store.openIncidents().length : '—' }} OPEN</span
      >
    </div>
    <div class="tabs" aria-label="Incident views">
      <a
        routerLink="/incidents"
        [class.active]="!all()"
        [attr.aria-current]="!all() ? 'page' : null"
        >Open incidents
        <span>{{ store.lastUpdated() ? store.openIncidents().length : '—' }}</span></a
      >
      <a
        routerLink="/incidents"
        [queryParams]="{ view: 'all' }"
        [class.active]="all()"
        [attr.aria-current]="all() ? 'page' : null"
        >All incidents <span>{{ store.lastUpdated() ? store.incidents().length : '—' }}</span></a
      >
    </div>
    <section class="panel">
      <div class="panel-heading">
        <div>
          <span class="eyebrow">INCIDENT REGISTER</span>
          <h2>{{ all() ? 'All incidents' : 'Open incidents' }}</h2>
        </div>
        <span class="muted">Newest first</span>
      </div>
      @if (store.lastUpdated()) {
        <app-incident-table
          [incidents]="visible()"
          [emptyTitle]="all() ? 'No incidents recorded' : 'No open incidents'"
          [emptyMessage]="
            all()
              ? 'Critical conditions will be recorded here.'
              : 'There are no incidents awaiting resolution in this snapshot.'
          "
        />
      } @else {
        <p class="loading-state" role="status">
          {{
            store.loading()
              ? 'Loading incidents…'
              : 'Incident data unavailable. Use Refresh to retry.'
          }}
        </p>
      }
    </section>
    <p class="form-hint">
      Resolving an incident records its closure. A later critical reading may open a new incident
      for the machine.
    </p>
  `,
})
export class Incidents {
  readonly store = inject(MonitoringStore);
  private readonly params = toSignal(inject(ActivatedRoute).queryParamMap);
  readonly all = computed(() => this.params()?.get('view') === 'all');
  readonly visible = computed(() =>
    this.all()
      ? this.store.recent()
      : [...this.store.openIncidents()].sort(
          (a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id,
        ),
  );
}
