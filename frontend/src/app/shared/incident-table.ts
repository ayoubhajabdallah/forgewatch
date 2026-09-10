import { DatePipe } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Incident } from '../core/api.models';
import { MonitoringStore } from '../core/monitoring.store';
import { Badge } from './badge';
@Component({
  selector: 'app-incident-table',
  standalone: true,
  imports: [DatePipe, RouterLink, Badge],
  template: `
    @if (incidents().length) {
      <div class="table-scroll">
        <table>
          <caption class="sr-only">
            Incidents, most recent first
          </caption>
          <thead>
            <tr>
              <th scope="col">Incident / machine</th>
              <th scope="col">Severity</th>
              <th scope="col">Status</th>
              <th scope="col">Created</th>
              <th scope="col">Action</th>
            </tr>
          </thead>
          <tbody>
            @for (incident of incidents(); track incident.id) {
              <tr>
                <td class="incident-description">
                  <strong>#{{ incident.id }} · {{ incident.description }}</strong
                  ><a [routerLink]="['/machines', incident.machineId]">{{
                    incident.machineName
                  }}</a>
                </td>
                <td><app-badge [value]="incident.severity" /></td>
                <td><app-badge [value]="incident.status" /></td>
                <td class="date-cell">
                  {{ incident.createdAt | date: 'MMM d, HH:mm' }}
                  @if (incident.resolvedAt) {
                    <small>Resolved {{ incident.resolvedAt | date: 'MMM d, HH:mm' }}</small>
                  }
                </td>
                <td>
                  @if (incident.status === 'OPEN') {
                    <button
                      class="button small"
                      [disabled]="store.resolving().has(incident.id)"
                      [attr.aria-label]="'Resolve incident ' + incident.id"
                      (click)="store.resolve(incident.id)"
                    >
                      {{ store.resolving().has(incident.id) ? 'Resolving…' : 'Resolve' }}
                    </button>
                  } @else {
                    <span class="muted">Closed</span>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else {
      <div class="empty">
        <span class="empty-mark" aria-hidden="true">✓</span><strong>{{ emptyTitle() }}</strong>
        <p>{{ emptyMessage() }}</p>
      </div>
    }
  `,
})
export class IncidentTable {
  readonly store = inject(MonitoringStore);
  readonly incidents = input.required<Incident[]>();
  readonly emptyTitle = input('No incidents to display');
  readonly emptyMessage = input(
    'Incidents will appear here when a critical condition is detected.',
  );
}
