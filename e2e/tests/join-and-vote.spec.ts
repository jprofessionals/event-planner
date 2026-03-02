import { test, expect } from '@playwright/test';

test.describe('Join and Vote', () => {
	let eventId: string;
	let adminToken: string;

	test.beforeEach(async ({ request }) => {
		const res = await request.post('/api/events', {
			data: {
				title: 'Vote Test Event',
				passphrase: 'testpass',
			},
		});
		const event = (await res.json()) as { id: string; adminToken: string };
		eventId = event.id;
		adminToken = event.adminToken;

		await request.post(`/api/events/${eventId}/time-options`, {
			headers: { 'X-Admin-Token': adminToken },
			data: {
				options: [
					{ startTime: '2026-03-15T18:00:00Z', endTime: '2026-03-15T20:00:00Z' },
					{ startTime: '2026-03-16T18:00:00Z', endTime: '2026-03-16T20:00:00Z' },
				],
			},
		});
	});

	test('should join event with passphrase and vote on time slots', async ({ page }) => {
		await page.goto(`/event/${eventId}/join`);

		await page.fill('#display-name', 'Alice');
		await page.fill('#passphrase', 'testpass');
		await page.click('button[type="submit"]');

		await expect(page).toHaveURL(`/event/${eventId}`);

		// Should see Vote/Overview toggle and the grid
		await expect(page.locator('button:has-text("Vote")').first()).toBeVisible();
		await expect(page.locator('button:has-text("Overview")').first()).toBeVisible();
	});

	test('should reject wrong passphrase', async ({ page }) => {
		await page.goto(`/event/${eventId}/join`);

		await page.fill('#display-name', 'Bob');
		await page.fill('#passphrase', 'wrongpass');
		await page.click('button[type="submit"]');

		await expect(page).toHaveURL(`/event/${eventId}/join`);
	});
});
