import { expect, test } from '@playwright/test';

test('sensor thresholds retain small values from the API', async ({ page }) => {
  await page.route('**/api/machines', (route) =>
    route.fulfill({
      json: [
        {
          id: 901,
          name: 'Precision check',
          location: 'Lab',
          status: 'IDLE',
          createdAt: '2026-09-10T10:00:00',
        },
      ],
    }),
  );
  await page.route('**/api/incidents**', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/machines/901/sensors', (route) =>
    route.fulfill({
      json: [
        {
          id: 902,
          machineId: 901,
          name: 'Fine vibration',
          type: 'VIBRATION',
          unit: 'm',
          warningThreshold: 0.0000001,
          criticalThreshold: 0.0000002,
        },
      ],
    }),
  );
  await page.goto('/');
  await expect(page.getByRole('cell', { name: '1e-7 m', exact: true })).toBeVisible();
  await expect(page.getByRole('cell', { name: '2e-7 m', exact: true })).toBeVisible();
});

test('dashboard reads the live API and fits desktop and mobile screens', async ({
  page,
  request,
}) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  const machinesResponse = await request.get('/api/machines');
  expect(machinesResponse.ok()).toBeTruthy();
  const machines = await machinesResponse.json();
  const open = await (await request.get('/api/incidents/open')).json();
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Operations overview' })).toBeVisible();
  await expect(page.getByTestId('total-machines')).toHaveText(String(machines.length));
  await expect(page.getByTestId('open-incidents')).toHaveText(String(open.length));
  await expect(page.getByTestId('critical-incidents')).toHaveText(
    String(open.filter((i: { severity: string }) => i.severity === 'CRITICAL').length),
  );
  if (machines.length) {
    await expect(page.getByRole('heading', { name: 'Thresholds & measurements' })).toBeVisible();
    await expect(page.getByText('Loading sensors…')).toHaveCount(0);
  }
  await page.screenshot({ path: 'test-results/overview-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
  ).toBeTruthy();
  await page.screenshot({ path: 'test-results/overview-mobile.png', fullPage: true });
  await page
    .getByRole('navigation', { name: 'Main navigation' })
    .getByRole('link', { name: /Incidents/ })
    .click();
  await expect(page.getByRole('heading', { name: 'Open incidents', exact: true })).toBeVisible();
  await page.getByRole('link', { name: /All incidents/ }).click();
  await expect(page.getByRole('heading', { name: 'All incidents', exact: true })).toBeVisible();
  expect(errors).toEqual([]);
});

test('unavailable API does not display misleading zero counts and can recover', async ({
  page,
}) => {
  await page.route('**/api/machines', (route) =>
    route.fulfill({ status: 503, body: 'Unavailable' }),
  );
  await page.goto('/');
  await expect(page.getByRole('alert')).toContainText('Unable to reach ForgeWatch');
  await expect(page.getByTestId('total-machines')).toHaveText('—');
  await expect(page.getByText('Workspace data unavailable')).toBeVisible();
  await page.unroute('**/api/machines');
  await page.getByRole('button', { name: /Refresh/ }).click();
  await expect(page.getByTestId('total-machines')).not.toHaveText('—');
  await expect(page.getByRole('alert')).toHaveCount(0);
  // A failed later refresh must preserve the snapshot and disclose that it is stale.
  const count = await page.getByTestId('total-machines').innerText();
  await page.route('**/api/machines', (route) =>
    route.fulfill({ status: 503, body: 'Unavailable' }),
  );
  await page.getByRole('button', { name: /Refresh/ }).click();
  await expect(page.getByRole('alert')).toContainText('last successful snapshot');
  await expect(page.getByTestId('total-machines')).toHaveText(count);
});

test('empty workspace offers a clear path to the first machine', async ({ page }) => {
  await page.route('**/api/**', (route) => route.fulfill({ json: [] }));
  await page.goto('/');
  await expect(page.getByTestId('total-machines')).toHaveText('0');
  await expect(page.getByText('Your workspace is ready')).toBeVisible();
  await page.getByRole('link', { name: /Add a machine/ }).click();
  await expect(page.getByText('No machines yet', { exact: true })).toBeVisible();
  await page.locator('app-create-machine summary').click();
  await expect(page.getByRole('form', { name: 'Create machine' })).toBeVisible();
});

test('live create machine, sensor, critical measurement and resolve workflow', async ({
  page,
  request,
}) => {
  test.skip(
    process.env['FORGEWATCH_LIVE_TEST'] !== '1',
    'Opt in: creates persistent, clearly named demo records.',
  );
  const name = `Frontend smoke ${Date.now()}`;
  await page.goto('/machines');
  await page.locator('app-create-machine summary').click();
  const machineForm = page.getByRole('form', { name: 'Create machine' });
  await machineForm.getByLabel('Machine name').fill(name);
  await machineForm.getByLabel('Location', { exact: true }).fill('Frontend validation');
  const createdMachine = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/machines') && response.request().method() === 'POST',
  );
  await machineForm.getByRole('button', { name: 'Create machine', exact: true }).click();
  const machineResponse = await createdMachine;
  expect(machineResponse.status()).toBe(201);
  const machine = await machineResponse.json();
  await expect(page).toHaveURL(new RegExp('/machines/' + machine.id + '$'));
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();

  await page.locator('app-sensor-panel summary').filter({ hasText: 'Create sensor' }).click();
  const sensorForm = page.getByRole('form', { name: 'Create sensor' });
  await sensorForm.getByLabel('Sensor name').fill('Smoke temperature');
  await sensorForm.getByLabel('Unit', { exact: true }).fill('°C');
  await sensorForm.getByLabel('Warning threshold').fill('80');
  await sensorForm.getByLabel('Critical threshold').fill('70');
  await sensorForm.getByRole('button', { name: 'Create sensor', exact: true }).click();
  await expect(sensorForm.getByRole('alert')).toHaveText(
    'Critical threshold must be greater than warning threshold.',
  );
  await sensorForm.getByLabel('Critical threshold').fill('100');
  const createdSensor = page.waitForResponse(
    (response) => response.url().endsWith('/sensors') && response.request().method() === 'POST',
  );
  await sensorForm.getByRole('button', { name: 'Create sensor', exact: true }).click();
  const sensorResponse = await createdSensor;
  expect(sensorResponse.status()).toBe(201);
  const sensor = await sensorResponse.json();
  await expect(page.getByRole('cell', { name: '100 °C', exact: true })).toBeVisible();

  await page.locator('app-sensor-panel summary').filter({ hasText: 'Submit measurement' }).click();
  const measurementForm = page.getByRole('form', { name: 'Submit measurement' });
  await measurementForm.getByLabel('Measurement value', { exact: false }).fill('100');
  const measurement = page.waitForResponse(
    (response) =>
      response.url().endsWith('/measurements') && response.request().method() === 'POST',
  );
  await measurementForm.getByRole('button', { name: 'Submit measurement', exact: true }).click();
  expect((await measurement).status()).toBe(201);
  const open = await (await request.get('/api/incidents/open')).json();
  const incident = open.find((item: { machineId: number }) => item.machineId === machine.id);
  expect(incident).toBeTruthy();
  expect(incident.severity).toBe('CRITICAL');
  await page
    .getByRole('navigation')
    .getByRole('link', { name: /Incidents/ })
    .click();
  const row = page.getByRole('row').filter({ hasText: name });
  await expect(row).toContainText('OPEN');
  await row.getByRole('button', { name: 'Resolve incident ' + incident.id, exact: true }).click();
  await expect(page.getByRole('row').filter({ hasText: name })).toHaveCount(0);
  await page.getByRole('link', { name: /All incidents/ }).click();
  await expect(page.getByRole('row').filter({ hasText: name })).toContainText('RESOLVED');
  const resolved = await (await request.get('/api/incidents/' + incident.id)).json();
  expect(resolved.status).toBe('RESOLVED');
  expect(resolved.resolvedAt).toBeTruthy();
  await page.reload();
  await expect(page.getByRole('row').filter({ hasText: name })).toContainText('RESOLVED');
  console.log(
    JSON.stringify({
      machineId: machine.id,
      sensorId: sensor.id,
      incidentId: incident.id,
      status: resolved.status,
    }),
  );
});
