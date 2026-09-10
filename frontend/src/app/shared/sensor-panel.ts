import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Machine, Sensor, SensorType } from '../core/api.models';
import { ApiService, apiError } from '../core/api.service';
import { MonitoringStore } from '../core/monitoring.store';
import { finiteNumber, thresholdOrder, trimmedRequired } from './validators';

@Component({
  selector: 'app-sensor-panel',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './sensor-panel.html',
})
export class SensorPanel {
  readonly machine = input.required<Machine>();
  readonly revision = input<Date | null>(null);
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  readonly store = inject(MonitoringStore);
  private request = 0;
  private currentMachine = 0;
  readonly sensors = signal<Sensor[]>([]);
  readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  readonly error = signal('');
  readonly formError = signal('');
  readonly success = signal('');
  readonly saving = signal(false);
  readonly selectedId = signal<number | null>(null);
  readonly selected = computed(
    () => this.sensors().find((s) => s.id === this.selectedId()) ?? null,
  );
  readonly types: SensorType[] = ['TEMPERATURE', 'PRESSURE', 'VIBRATION', 'POWER', 'COOLING_FLOW'];
  readonly sensorForm = this.fb.group(
    {
      name: this.fb.nonNullable.control('', [trimmedRequired, Validators.maxLength(100)]),
      type: this.fb.nonNullable.control<SensorType>('TEMPERATURE', Validators.required),
      unit: this.fb.nonNullable.control('', [trimmedRequired, Validators.maxLength(20)]),
      warningThreshold: this.fb.control<number | null>(null, finiteNumber),
      criticalThreshold: this.fb.control<number | null>(null, finiteNumber),
    },
    { validators: thresholdOrder },
  );
  readonly measurementForm = this.fb.group({
    value: this.fb.control<number | null>(null, finiteNumber),
  });

  constructor() {
    effect(() => {
      const id = this.machine().id;
      this.revision();
      untracked(() => {
        if (this.currentMachine !== id) {
          this.currentMachine = id;
          this.selectedId.set(null);
          this.sensorForm.reset();
          this.measurementForm.reset();
          this.formError.set('');
          this.success.set('');
        }
        void this.load();
      });
    });
  }
  async load() {
    const id = this.machine().id;
    const request = ++this.request;
    this.state.set('loading');
    this.error.set('');
    this.sensors.set([]);
    try {
      const sensors = await firstValueFrom(this.api.sensors(id));
      if (request !== this.request) return;
      this.sensors.set(sensors);
      if (!sensors.some((s) => s.id === this.selectedId()))
        this.selectedId.set(sensors[0]?.id ?? null);
      this.state.set('ready');
    } catch (error) {
      if (request !== this.request) return;
      this.error.set(apiError(error));
      this.state.set('error');
    }
  }
  select(value: string) {
    this.selectedId.set(Number(value));
    this.measurementForm.reset();
    this.success.set('');
  }
  async createSensor() {
    this.sensorForm.markAllAsTouched();
    if (this.sensorForm.invalid || this.saving()) return;
    const id = this.machine().id;
    const values = this.sensorForm.getRawValue();
    this.saving.set(true);
    this.formError.set('');
    this.success.set('');
    try {
      const sensor = await firstValueFrom(
        this.api.createSensor(id, {
          name: values.name.trim(),
          type: values.type,
          unit: values.unit.trim(),
          warningThreshold: values.warningThreshold!,
          criticalThreshold: values.criticalThreshold!,
        }),
      );
      this.store.notice.set(`Sensor “${sensor.name}” created.`);
      if (id === this.machine().id) {
        this.sensorForm.reset();
        this.selectedId.set(sensor.id);
        await this.load();
      }
    } catch (error) {
      if (id === this.machine().id) this.formError.set(apiError(error));
    } finally {
      this.saving.set(false);
    }
  }
  async submitMeasurement() {
    this.measurementForm.markAllAsTouched();
    const sensor = this.selected();
    if (this.measurementForm.invalid || !sensor || this.saving()) return;
    this.saving.set(true);
    this.formError.set('');
    this.success.set('');
    try {
      const result = await firstValueFrom(
        this.api.measure(sensor.id, this.measurementForm.getRawValue().value!),
      );
      const message = `Measurement saved: ${result.value} ${sensor.unit} on ${sensor.name}.`;
      this.store.notice.set(message);
      if (sensor.machineId === this.machine().id) {
        this.success.set(message);
        this.measurementForm.reset();
      }
      this.store.refresh();
    } catch (error) {
      if (sensor.machineId === this.machine().id) this.formError.set(apiError(error));
    } finally {
      this.saving.set(false);
    }
  }
}
