<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { auth as authApi, type EventResponse } from '$lib/api/client';
	import { auth } from '$lib/stores/auth';
	import { get } from 'svelte/store';
	import { onMount } from 'svelte';

	let eventList = $state<EventResponse[]>([]);
	let loading = $state(true);
	let error = $state('');

	async function loadEvents() {
		const authState = get(auth);
		if (!authState.token) {
			void goto(resolve('/login'));
			return;
		}

		loading = true;
		try {
			eventList = await authApi.myEvents(authState.token);
		} catch {
			error = 'Failed to load your events.';
		} finally {
			loading = false;
		}
	}

	function handleLogout() {
		auth.logout();
		void goto(resolve('/login'));
	}

	function formatDate(iso: string): string {
		return new Date(iso).toLocaleDateString(undefined, {
			year: 'numeric',
			month: 'short',
			day: 'numeric',
		});
	}

	onMount(() => {
		const authState = get(auth);
		if (!authState.token) {
			void goto(resolve('/login'));
			return;
		}
		void loadEvents();
	});
</script>

<div class="container mx-auto max-w-4xl p-8 space-y-6">
	<div class="flex items-center justify-between">
		<h1 class="h2">My Events</h1>
		<div class="flex gap-2">
			<a href="{resolve('/')}?create" class="btn preset-filled-primary-500">Create Event</a>
			<button class="btn preset-tonal" onclick={handleLogout}>Log Out</button>
		</div>
	</div>

	{#if loading}
		<div class="flex items-center gap-3 opacity-75">
			<svg
				class="animate-spin h-5 w-5"
				xmlns="http://www.w3.org/2000/svg"
				fill="none"
				viewBox="0 0 24 24"
			>
				<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"
				></circle>
				<path
					class="opacity-75"
					fill="currentColor"
					d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
				></path>
			</svg>
			<span>Loading your events...</span>
		</div>
	{:else if error}
		<p class="text-error-500">{error}</p>
	{:else if eventList.length === 0}
		<div class="text-center py-12 space-y-4">
			<p class="text-lg opacity-75">You don't have any events yet.</p>
			<a href="{resolve('/')}?create" class="btn preset-filled-primary-500"
				>Create Your First Event</a
			>
		</div>
	{:else}
		<div class="grid gap-4 md:grid-cols-2">
			{#each eventList as event (event.id)}
				<a
					href="{resolve('/event/[eventId]', { eventId: event.id })}{event.adminToken
						? `#key=${event.adminToken}`
						: ''}"
					class="card bg-surface-100-900 p-4 space-y-2 shadow-xl hover:ring-2 hover:ring-primary-500 transition-all block"
				>
					<div class="flex items-center gap-2">
						<h3 class="h4">{event.title}</h3>
						{#if event.adminToken}
							<span class="badge preset-filled-warning-500">Admin</span>
						{/if}
					</div>
					{#if event.description}
						<p class="text-sm opacity-75 line-clamp-2">{event.description}</p>
					{/if}
					{#if event.decidedTimeStart}
						<p class="text-sm font-semibold">
							Decided: {new Date(event.decidedTimeStart).toLocaleString()}
						</p>
					{/if}
					<p class="text-xs opacity-50">Created {formatDate(event.createdAt)}</p>
				</a>
			{/each}
		</div>
	{/if}
</div>
