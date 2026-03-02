<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { events, ApiError } from '$lib/api/client';
	import { setParticipantSession } from '$lib/stores/event';
	import { auth } from '$lib/stores/auth';
	import { get } from 'svelte/store';
	import PassphraseGate from '$lib/components/PassphraseGate.svelte';
	import type { EventResponse } from '$lib/api/client';

	const eventId = $derived(page.params.eventId!);

	let event = $state<EventResponse | null>(null);
	let loading = $state(true);
	let joinLoading = $state(false);
	let error = $state('');
	let joinError = $state('');

	async function loadEvent() {
		loading = true;
		try {
			event = await events.get(eventId);
		} catch {
			error = 'Event not found.';
		} finally {
			loading = false;
		}
	}

	async function handleJoin(passphrase: string, displayName: string) {
		joinLoading = true;
		joinError = '';
		try {
			const authState = get(auth);
			const result = await events.join(
				eventId,
				{ passphrase, displayName },
				authState.token ?? undefined,
			);
			setParticipantSession({
				participantId: result.participantId,
				displayName: result.displayName,
				eventId: result.eventId,
			});
			void goto(resolve('/event/[eventId]', { eventId }));
		} catch (err) {
			if (err instanceof ApiError) {
				if (err.status === 403 || err.status === 401) {
					joinError = 'Wrong passphrase. Please try again.';
				} else {
					joinError = `Failed to join: ${err.body}`;
				}
			} else {
				joinError = 'Failed to join event. Please try again.';
			}
		} finally {
			joinLoading = false;
		}
	}

	$effect(() => {
		void loadEvent();
	});
</script>

<div class="container mx-auto max-w-md p-8 space-y-6">
	{#if loading}
		<p class="opacity-75">Loading event...</p>
	{:else if error}
		<div class="text-center space-y-4">
			<h1 class="h2">Event Not Found</h1>
			<p class="text-error-500">{error}</p>
			<a href={resolve('/')} class="btn preset-tonal">Go Home</a>
		</div>
	{:else if event}
		<header class="text-center space-y-2">
			<h1 class="h2">Join Event</h1>
			<p class="text-lg font-semibold">{event.title}</p>
			{#if event.description}
				<p class="opacity-75">{event.description}</p>
			{/if}
		</header>

		<div class="card bg-surface-100-900 p-6 shadow-xl">
			<PassphraseGate
				onSubmit={handleJoin}
				error={joinError}
				loading={joinLoading}
				prefillName={get(auth).user?.displayName ?? ''}
			/>
		</div>
	{/if}
</div>
