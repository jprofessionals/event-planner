import { defineConfig } from '@playwright/test';

export default defineConfig({
	testDir: './tests',
	timeout: 60000,
	retries: 0,
	use: {
		baseURL: process.env.FRONTEND_URL || 'http://localhost:4173',
		headless: true,
		screenshot: 'only-on-failure',
	},
	projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
	globalSetup: process.env.CI ? undefined : './global-setup.ts',
	globalTeardown: process.env.CI ? undefined : './global-teardown.ts',
});
