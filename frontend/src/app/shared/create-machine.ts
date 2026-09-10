import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService, apiError } from '../core/api.service';
import { Machine } from '../core/api.models';
import { MonitoringStore } from '../core/monitoring.store';
import { trimmedRequired } from './validators';
@Component({
  selector: 'app-create-machine',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <details class="form-disclosure">
      <summary>＋ Create machine</summary>
      <form [formGroup]="form" (ngSubmit)="submit()" aria-label="Create machine">
        <p class="muted">Register a machine and its location. New machines start as idle.</p>
        <fieldset [disabled]="saving()">
          <div class="form-grid">
            <label
              >Machine name<input
                formControlName="name"
                maxlength="100"
                placeholder="e.g. Induction Furnace 03"
                required
            /></label>
            <label
              >Location<input
                formControlName="location"
                maxlength="150"
                placeholder="e.g. Production Hall B"
                required
            /></label>
          </div>
          @if (form.touched && form.invalid) {
            <p class="field-error" role="alert">
              Enter a machine name (up to 100 characters) and location (up to 150).
            </p>
          }
          <button class="button primary" type="submit">
            {{ saving() ? 'Creating…' : 'Create machine' }}
          </button>
        </fieldset>
        @if (error()) {
          <p class="field-error" role="alert">{{ error() }}</p>
        }
      </form>
    </details>
  `,
})
export class CreateMachineForm {
  private readonly api = inject(ApiService);
  private readonly store = inject(MonitoringStore);
  private readonly fb = inject(FormBuilder);
  readonly created = output<Machine>();
  readonly saving = signal(false);
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({
    name: ['', [trimmedRequired, Validators.maxLength(100)]],
    location: ['', [trimmedRequired, Validators.maxLength(150)]],
  });
  async submit() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    const { name, location } = this.form.getRawValue();
    try {
      const machine = await firstValueFrom(
        this.api.createMachine({ name: name.trim(), location: location.trim() }),
      );
      this.form.reset();
      this.store.notice.set(`Machine “${machine.name}” created.`);
      this.store.refresh();
      this.created.emit(machine);
    } catch (error) {
      this.error.set(apiError(error));
    } finally {
      this.saving.set(false);
    }
  }
}
