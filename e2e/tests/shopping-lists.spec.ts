import { test, expect } from '@playwright/test';

test.describe('Shopping Lists', () => {
	let eventId: string;
	let adminToken: string;

	test.beforeEach(async ({ request }) => {
		// Create event and transition to planning
		const res = await request.post('/api/events', {
			data: { title: 'Shopping Test', passphrase: 'shoppass' },
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

	test('admin sees Shopping tab in planning stage', async ({ page }) => {
		await page.goto(`/event/${eventId}#key=${adminToken}`);

		// Admin name prompt
		await page.fill('#admin-name', 'Admin');
		await page.click('button[type="submit"]');

		// Should see Shopping tab
		await expect(page.getByRole('tab', { name: 'Shopping' })).toBeVisible();
	});

	test('Shopping tab shows empty state and Add button for admin', async ({ page }) => {
		await page.goto(`/event/${eventId}#key=${adminToken}`);

		await page.fill('#admin-name', 'Admin');
		await page.click('button[type="submit"]');

		// Click Shopping tab
		await page.getByRole('tab', { name: 'Shopping' }).click();

		// Should see empty state
		await expect(page.getByText('No shopping lists yet.')).toBeVisible();

		// Should see Add button (admin always can)
		await expect(page.getByRole('button', { name: 'Add Shopping List' })).toBeVisible();
	});

	test('participant without permission cannot see Add button', async ({ page }) => {
		await page.goto(`/event/${eventId}/join`);
		await page.fill('#display-name', 'Bob');
		await page.fill('#passphrase', 'shoppass');
		await page.click('button[type="submit"]');

		// Click Shopping tab
		await page.getByRole('tab', { name: 'Shopping' }).click();

		// Should NOT see Add button (participants_can_shopping_list defaults to false)
		await expect(page.getByRole('button', { name: 'Add Shopping List' })).not.toBeVisible();
	});

	test('admin toggle enables participant shopping list creation', async ({ page, request }) => {
		// Enable participant shopping list permission
		await request.patch(`/api/events/${eventId}`, {
			headers: { 'X-Admin-Token': adminToken },
			data: { participantsCanShoppingList: true },
		});

		await page.goto(`/event/${eventId}/join`);
		await page.fill('#display-name', 'Alice');
		await page.fill('#passphrase', 'shoppass');
		await page.click('button[type="submit"]');

		// Click Shopping tab
		await page.getByRole('tab', { name: 'Shopping' }).click();

		// Should see Add button now
		await expect(page.getByRole('button', { name: 'Add Shopping List' })).toBeVisible();
	});
});
