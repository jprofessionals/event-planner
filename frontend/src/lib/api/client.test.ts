import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ApiError, events, scheduling, polls, checklist, comments, auth } from './client';

const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

function jsonResponse(data: unknown, status = 200) {
	return new Response(JSON.stringify(data), {
		status,
		headers: { 'Content-Type': 'application/json' },
	});
}

function errorResponse(status: number, body = '') {
	return new Response(body, { status });
}

beforeEach(() => {
	mockFetch.mockReset();
});

describe('ApiError', () => {
	it('stores status and body', () => {
		const err = new ApiError(404, 'not found');
		expect(err.status).toBe(404);
		expect(err.body).toBe('not found');
		expect(err.message).toBe('API Error 404');
	});
});

describe('events', () => {
	it('create sends POST and returns response', async () => {
		const event = { id: '1', title: 'Test', adminToken: 'tok' };
		mockFetch.mockResolvedValueOnce(jsonResponse(event, 201));

		const result = await events.create({ title: 'Test', passphrase: 'pass' });

		expect(result).toEqual(event);
		expect(mockFetch).toHaveBeenCalledWith(
			'/api/events',
			expect.objectContaining({
				method: 'POST',
				body: JSON.stringify({ title: 'Test', passphrase: 'pass' }),
			}),
		);
	});

	it('get fetches event by id', async () => {
		const event = { id: '1', title: 'Test' };
		mockFetch.mockResolvedValueOnce(jsonResponse(event));

		const result = await events.get('1');

		expect(result).toEqual(event);
		expect(mockFetch).toHaveBeenCalledWith(
			'/api/events/1',
			expect.objectContaining({ method: 'GET' }),
		);
	});

	it('update sends PATCH with admin token header', async () => {
		mockFetch.mockResolvedValueOnce(jsonResponse({ id: '1', title: 'Updated' }));

		await events.update('1', { title: 'Updated' }, 'admin-token');

		/* eslint-disable @typescript-eslint/no-unsafe-assignment */
		const expected = expect.objectContaining({
			method: 'PATCH',
			headers: expect.objectContaining({ 'X-Admin-Token': 'admin-token' }),
		});
		/* eslint-enable @typescript-eslint/no-unsafe-assignment */
		expect(mockFetch).toHaveBeenCalledWith('/api/events/1', expected);
	});

	it('join sends passphrase and display name', async () => {
		mockFetch.mockResolvedValueOnce(
			jsonResponse({ participantId: 'p1', displayName: 'Alice', eventId: '1' }),
		);

		const result = await events.join('1', { passphrase: 'secret', displayName: 'Alice' });

		expect(result.displayName).toBe('Alice');
	});

	it('throws ApiError on non-ok response', async () => {
		mockFetch.mockResolvedValueOnce(errorResponse(403, 'forbidden'));

		try {
			await events.get('1');
			expect.unreachable('should have thrown');
		} catch (err) {
			expect(err).toBeInstanceOf(ApiError);
			expect((err as ApiError).status).toBe(403);
		}
	});
});

describe('scheduling', () => {
	it('addTimeOptions sends admin token', async () => {
		mockFetch.mockResolvedValueOnce(jsonResponse([], 201));

		await scheduling.addTimeOptions(
			'ev1',
			{
				options: [{ startTime: '2026-03-15T18:00:00Z', endTime: '2026-03-15T20:00:00Z' }],
			},
			'admin-tok',
		);

		/* eslint-disable @typescript-eslint/no-unsafe-assignment */
		const expected = expect.objectContaining({
			method: 'POST',
			headers: expect.objectContaining({ 'X-Admin-Token': 'admin-tok' }),
		});
		/* eslint-enable @typescript-eslint/no-unsafe-assignment */
		expect(mockFetch).toHaveBeenCalledWith('/api/events/ev1/time-options', expected);
	});

	it('getTimeOptions fetches options', async () => {
		mockFetch.mockResolvedValueOnce(
			jsonResponse([{ id: '1', startTime: 'a', endTime: 'b', votes: [] }]),
		);

		const result = await scheduling.getTimeOptions('ev1');

		expect(result).toHaveLength(1);
	});
});

describe('polls', () => {
	it('create sends poll data with admin token', async () => {
		mockFetch.mockResolvedValueOnce(jsonResponse({ id: 'p1', question: 'Food?' }));

		await polls.create('ev1', { question: 'Food?', options: ['Pizza', 'Tacos'] }, 'admin');

		expect(mockFetch).toHaveBeenCalledWith(
			'/api/events/ev1/polls',
			expect.objectContaining({
				method: 'POST',
			}),
		);
	});
});

describe('checklist', () => {
	it('add creates a checklist item', async () => {
		mockFetch.mockResolvedValueOnce(
			jsonResponse({ id: 'c1', text: 'Buy snacks', completed: false }),
		);

		const result = await checklist.add('ev1', { text: 'Buy snacks' });

		expect(result.text).toBe('Buy snacks');
		expect(result.completed).toBe(false);
	});
});

describe('comments', () => {
	it('add posts a comment', async () => {
		mockFetch.mockResolvedValueOnce(
			jsonResponse({ id: 'cm1', authorName: 'Alice', content: 'Hello!' }),
		);

		const result = await comments.add('ev1', { authorName: 'Alice', content: 'Hello!' });

		expect(result.authorName).toBe('Alice');
	});
});

describe('auth', () => {
	it('login sends credentials', async () => {
		mockFetch.mockResolvedValueOnce(
			jsonResponse({ token: 'tok', user: { id: '1', email: 'a@b.c', displayName: 'A' } }),
		);

		const result = await auth.login({ email: 'a@b.c', password: 'pass' });

		expect(result.token).toBe('tok');
	});

	it('me sends bearer token', async () => {
		mockFetch.mockResolvedValueOnce(jsonResponse({ id: '1', email: 'a@b.c', displayName: 'A' }));

		await auth.me('my-token');

		/* eslint-disable @typescript-eslint/no-unsafe-assignment */
		const expected = expect.objectContaining({
			headers: expect.objectContaining({ Authorization: 'Bearer my-token' }),
		});
		/* eslint-enable @typescript-eslint/no-unsafe-assignment */
		expect(mockFetch).toHaveBeenCalledWith('/api/users/me', expected);
	});
});
