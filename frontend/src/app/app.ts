import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MonitoringStore } from './core/monitoring.store';
@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DatePipe],
  standalone: true,
  selector: 'app-root',
  templateUrl: './app.html',
})
export class App {
  readonly store = inject(MonitoringStore);
  constructor() {
    this.store.refresh();
  }
}
