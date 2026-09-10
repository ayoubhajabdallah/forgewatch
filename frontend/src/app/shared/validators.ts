import { AbstractControl, ValidationErrors } from '@angular/forms';
export function trimmedRequired(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.trim() ? null : { required: true };
}
export function finiteNumber(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'number' && Number.isFinite(control.value)
    ? null
    : { finite: true };
}
export function thresholdOrder(control: AbstractControl): ValidationErrors | null {
  const { warningThreshold, criticalThreshold } = control.value;
  return typeof warningThreshold === 'number' &&
    typeof criticalThreshold === 'number' &&
    criticalThreshold <= warningThreshold
    ? { thresholdOrder: true }
    : null;
}
