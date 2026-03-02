import { test, expect } from '@playwright/test';

test.describe('Event Creation', () => {
	test('should create an event and navigate to admin page', async ({ page }) => {
		await page.goto('/');

		// Click "Create Event" to reveal the form
		await page.click('button:has-text("Create Event")');

		await page.fill('#title', 'Game Night');
		await page.fill('#description', 'Let us play board games');
		await page.fill('#passphrase', 'fun times');
		await page.click('button[type="submit"]');

		// Should redirect to admin page
		await expect(page).toHaveURL(/\/event\/.*\/admin/);
		await expect(page.locator('h1')).toContainText('Game Night');
	});
});
