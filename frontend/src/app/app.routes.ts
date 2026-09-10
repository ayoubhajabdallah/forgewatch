import { Routes } from '@angular/router';
export const routes: Routes = [
  {
    path: '',
    title: 'Overview · ForgeWatch',
    loadComponent: () => import('./pages/overview').then((m) => m.Overview),
  },
  {
    path: 'machines',
    title: 'Machines · ForgeWatch',
    loadComponent: () => import('./pages/machines').then((m) => m.Machines),
  },
  {
    path: 'machines/:id',
    title: 'Machine · ForgeWatch',
    loadComponent: () => import('./pages/machines').then((m) => m.Machines),
  },
  {
    path: 'incidents',
    title: 'Incidents · ForgeWatch',
    loadComponent: () => import('./pages/incidents').then((m) => m.Incidents),
  },
  { path: '**', redirectTo: '' },
];
