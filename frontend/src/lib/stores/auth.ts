import { writable } from 'svelte/store';

export interface AuthState {
	token: string | null;
	user: { id: string; email: string; displayName: string } | null;
}

function createAuthStore() {
	const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('auth') : null;
	const initial: AuthState = stored
		? (JSON.parse(stored) as AuthState)
		: { token: null, user: null };
	const { subscribe, set } = writable<AuthState>(initial);

	return {
		subscribe,
		login: (token: string, user: AuthState['user']) => {
			const state = { token, user };
			localStorage.setItem('auth', JSON.stringify(state));
			set(state);
		},
		logout: () => {
			localStorage.removeItem('auth');
			set({ token: null, user: null });
		},
	};
}

export const auth = createAuthStore();
