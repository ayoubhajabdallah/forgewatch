import { Component, input } from '@angular/core';
@Component({
  selector: 'app-badge',
  standalone: true,
  template:
    '<span class="badge" [attr.data-status]="value()">{{ value().replaceAll("_", " ") }}</span>',
})
export class Badge {
  readonly value = input.required<string>();
}
