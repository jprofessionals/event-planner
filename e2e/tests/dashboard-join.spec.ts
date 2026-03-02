import { test, expect } from '@playwright/test';

interface AuthResponse {
	token: string;
	user: { id: string; email: string; displayName: string };
}

interface EventResponse {
	id: string;
	title: string;
	stage: string;
}

test.describe('Dashboard shows joined events', () => {
	const backendUrl = 'http://localhost:8080';

	test('joined event appears in user dashboard via API', async ({ request }) => {
		// 1. Register a user
		const regRes = await request.post(`${backendUrl}/api/auth/register`, {
			data: {
				email: `dashtest-${Date.now()}@test.com`,
				password: 'testpass123',
				displayName: 'Dashboard Tester',
			},
		});
		expect(regRes.ok()).toBeTruthy();
		const authData = (await regRes.json()) as AuthResponse;
		const token = authData.token;

		// 2. Create an event (anonymously, no auth header)
		const createRes = await request.post(`${backendUrl}/api/events`, {
			data: {
				title: 'Join Test Event',
				passphrase: 'secret123',
			},
		});
		expect(createRes.ok()).toBeTruthy();
		const event = (await createRes.json()) as EventResponse;

		// 3. Join the event WITH auth token
		const joinRes = await request.post(`${backendUrl}/api/events/${event.id}/join`, {
			headers: { Authorization: `Bearer ${token}` },
			data: {
				passphrase: 'secret123',
				displayName: 'Dashboard Tester',
			},
		});
		expect(joinRes.ok()).toBeTruthy();

		// 4. Check dashboard - the event should appear
		const eventsRes = await request.get(`${backendUrl}/api/users/me/events`, {
			headers: { Authorization: `Bearer ${token}` },
		});
		expect(eventsRes.ok()).toBeTruthy();
		const events = (await eventsRes.json()) as EventResponse[];

		const found = events.find((e) => e.id === event.id);
		expect(found).toBeTruthy();
		expect(found!.title).toBe('Join Test Event');
	});

	test('joined event appears via frontend proxy', async ({ request }) => {
		// 1. Register a user
		const regRes = await request.post('/api/auth/register', {
			data: {
				email: `proxytest-${Date.now()}@test.com`,
				password: 'testpass123',
				displayName: 'Proxy Tester',
			},
		});
		expect(regRes.ok()).toBeTruthy();
		const authData = (await regRes.json()) as AuthResponse;
		const token = authData.token;

		// 2. Create an event
		const createRes = await request.post('/api/events', {
			data: {
				title: 'Proxy Join Test',
				passphrase: 'secret456',
			},
		});
		expect(createRes.ok()).toBeTruthy();
		const event = (await createRes.json()) as EventResponse;

		// 3. Join with auth token via proxy
		const joinRes = await request.post(`/api/events/${event.id}/join`, {
			headers: { Authorization: `Bearer ${token}` },
			data: {
				passphrase: 'secret456',
				displayName: 'Proxy Tester',
			},
		});
		expect(joinRes.ok()).toBeTruthy();

		// 4. Check dashboard via proxy
		const eventsRes = await request.get('/api/users/me/events', {
			headers: { Authorization: `Bearer ${token}` },
		});
		expect(eventsRes.ok()).toBeTruthy();
		const events = (await eventsRes.json()) as EventResponse[];

		const found = events.find((e) => e.id === event.id);
		expect(found).toBeTruthy();
	});
});
