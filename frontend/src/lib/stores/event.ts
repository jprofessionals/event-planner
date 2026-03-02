import { writable } from 'svelte/store';
import type { EventResponse } from '$lib/api/client';

export interface ParticipantSession {
	participantId: string;
	displayName: string;
	eventId: string;
}

export interface EventState {
	event: EventResponse | null;
	loading: boolean;
	error: string | null;
}

function createEventStore() {
	const { subscribe, set, update } = writable<EventState>({
		event: null,
		loading: false,
		error: null,
	});

	return {
		subscribe,
		setEvent: (event: EventResponse) => {
			set({ event, loading: false, error: null });
		},
		setLoading: () => {
			update((s) => ({ ...s, loading: true, error: null }));
		},
		setError: (error: string) => {
			set({ event: null, loading: false, error });
		},
		reset: () => {
			set({ event: null, loading: false, error: null });
		},
	};
}

export const eventStore = createEventStore();

// Participant session - stored per event in sessionStorage
export function getParticipantSession(eventId: string): ParticipantSession | null {
	if (typeof sessionStorage === 'undefined') return null;
	const stored = sessionStorage.getItem(`participant:${eventId}`);
	return stored ? (JSON.parse(stored) as ParticipantSession) : null;
}

export function setParticipantSession(session: ParticipantSession): void {
	sessionStorage.setItem(`participant:${session.eventId}`, JSON.stringify(session));
}

export function clearParticipantSession(eventId: string): void {
	sessionStorage.removeItem(`participant:${eventId}`);
}

// Admin token - stored per event in sessionStorage
export function getAdminToken(eventId: string): string | null {
	if (typeof sessionStorage === 'undefined') return null;
	return sessionStorage.getItem(`admin:${eventId}`);
}

export function setAdminToken(eventId: string, token: string): void {
	sessionStorage.setItem(`admin:${eventId}`, token);
}
