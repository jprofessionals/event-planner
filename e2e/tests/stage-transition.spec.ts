import { test, expect } from '@playwright/test';

test.describe('Stage Transition', () => {
	test('admin can decide time and transition to planning stage', async ({ page, request }) => {
		// Create event
		const res = await request.post('/api/events', {
			data: { title: 'Transition Test', passphrase: 'pass' },
		});
		const event = (await res.json()) as { id: string; adminToken: string };

		// Add a time option
		const now = new Date();
		const startTime = new Date(now.getTime() + 24 * 60 * 60 * 1000).toISOString();
		const endTime = new Date(now.getTime() + 26 * 60 * 60 * 1000).toISOString();

		await request.post(`/api/events/${event.id}/time-options`, {
			headers: { 'X-Admin-Token': event.adminToken },
			data: { options: [{ startTime, endTime }] },
		});

		// Navigate to admin page
		await page.goto(`/event/${event.id}/admin#key=${event.adminToken}`);
		await expect(page.locator('h1')).toContainText('Transition Test');

		// Decide time via API (simulating the admin action)
		const decideRes = await request.post(`/api/events/${event.id}/decide`, {
			headers: { 'X-Admin-Token': event.adminToken },
			data: { startTime, endTime },
		});
		expect(decideRes.ok()).toBeTruthy();

		const decided = (await decideRes.json()) as { stage: string };
		expect(decided.stage).toBe('PLANNING');

		// Navigate to event page and verify planning stage UI
		await page.goto(`/event/${event.id}#key=${event.adminToken}`);

		// Enter display name to proceed past the name prompt
		await page.fill('#admin-name', 'Admin');
		await page.click('button[type="submit"]');

		// The page should show planning stage content (tabs for polls, checklist, etc.)
		// Wait for the page to load and verify we see planning-related content
		await expect(page.getByRole('tab', { name: 'Polls' })).toBeVisible({ timeout: 10000 });
	});
});
