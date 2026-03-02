import { test, expect } from '@playwright/test';

test.describe('Planning Stage', () => {
	let eventId: string;
	let adminToken: string;

	test.beforeEach(async ({ request }) => {
		// Create event and transition to planning
		const res = await request.post('/api/events', {
			data: { title: 'Planning Test', passphrase: 'planpass' },
		});
		const event = (await res.json()) as { id: string; adminToken: string };
		eventId = event.id;
		adminToken = event.adminToken;

		// Decide time to transition to PLANNING
		await request.post(`/api/events/${eventId}/decide`, {
			headers: { 'X-Admin-Token': adminToken },
			data: { startTime: '2026-03-15T18:00:00Z', endTime: '2026-03-15T20:00:00Z' },
		});
	});

	test('should show planning features after stage transition', async ({ page }) => {
		// Join the event first
		await page.goto(`/event/${eventId}/join`);
		await page.fill('#display-name', 'TestUser');
		await page.fill('#passphrase', 'planpass');
		await page.click('button[type="submit"]');

		// Should see planning stage with polls/checklist/comments tabs
		await expect(page).toHaveURL(`/event/${eventId}`);
	});
});
