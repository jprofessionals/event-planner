<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { untrack } from 'svelte';
	import { events, scheduling, type EventResponse, type TimeOptionResponse } from '$lib/api/client';
	import { getAdminToken, setAdminToken } from '$lib/stores/event';
	import { subscribeToEvent } from '$lib/api/sse';
	import EventHeader from '$lib/components/EventHeader.svelte';
	import SlotGrid from '$lib/components/SlotGrid.svelte';

	const eventId = $derived(page.params.eventId!);

	let event = $state<EventResponse | null>(null);
	let adminToken = $state<string | null>(null);
	let loading = $state(true);
	let error = $state('');

	// Edit form state
	let editMode = $state(false);
	let editTitle = $state('');
	let editDescription = $state('');
	let editPassphrase = $state('');
	let saving = $state(false);
	let editParticipantsCanPoll = $state(true);
	let editParticipantsCanChecklist = $state(true);
	let editParticipantsCanShoppingList = $state(false);

	// Time options
	let timeOptions = $state<TimeOptionResponse[]>([]);

	let unsubscribe: (() => void) | null = null;

	async function loadTimeOptions() {
		try {
			timeOptions = await scheduling.getTimeOptions(eventId, { adminToken: adminToken! });
		} catch {
			// Silently fail
		}
	}

	async function loadEvent() {
		const isInitialLoad = !event;
		if (isInitialLoad) loading = true;
		try {
			event = await events.get(eventId, adminToken!);
			await loadTimeOptions();
		} catch {
			error = 'Failed to load event.';
		} finally {
			if (isInitialLoad) loading = false;
		}
	}

	function setupSSE() {
		unsubscribe = subscribeToEvent(eventId, () => {
			void loadEvent();
		});
	}

	async function saveEdit() {
		if (!adminToken || !event) return;
		saving = true;
		try {
			event = await events.update(
				eventId,
				{
					title: editTitle.trim() || undefined,
					description: editDescription.trim() || undefined,
					passphrase: editPassphrase.trim() || undefined,
					participantsCanPoll: editParticipantsCanPoll,
					participantsCanChecklist: editParticipantsCanChecklist,
					participantsCanShoppingList: editParticipantsCanShoppingList,
				},
				adminToken,
			);
			editMode = false;
		} catch {
			error = 'Failed to save changes.';
		} finally {
			saving = false;
		}
	}

	function startEdit() {
		if (!event) return;
		editTitle = event.title;
		editDescription = event.description || '';
		editPassphrase = event.passphrase || '';
		editParticipantsCanPoll = event.participantsCanPoll;
		editParticipantsCanChecklist = event.participantsCanChecklist;
		editParticipantsCanShoppingList = event.participantsCanShoppingList;
		editMode = true;
	}

	function goToVoting() {
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- resolve used with hash fragment appended
		void goto(`${resolve('/event/[eventId]', { eventId })}#key=${adminToken}`);
	}

	$effect(() => {
		const hash = typeof window !== 'undefined' ? window.location.hash : '';
		const hashKey = hash.startsWith('#key=') ? hash.slice(5) : null;
		if (hashKey) {
			setAdminToken(eventId, hashKey);
			adminToken = hashKey;
			history.replaceState({}, '', window.location.pathname);
		} else {
			adminToken = getAdminToken(eventId);
		}

		if (!adminToken) {
			error = 'No admin token found. You need the admin link to manage this event.';
			loading = false;
			return;
		}

		untrack(() => {
			void loadEvent();
		});

		unsubscribe?.();
		setupSSE();

		return () => {
			unsubscribe?.();
		};
	});
</script>

<div class="container mx-auto max-w-4xl p-8 space-y-6">
	{#if loading}
		<p class="opacity-75">Loading event...</p>
	{:else if !adminToken}
		<div class="text-center space-y-4">
			<h1 class="h2">Access Denied</h1>
			<p class="text-error-500">{error}</p>
			<a href={resolve('/')} class="btn preset-tonal">Go Home</a>
		</div>
	{:else if event}
		<div class="flex items-center gap-2 mb-2">
			<a href={resolve('/')} class="btn btn-sm preset-tonal">Home</a>
			<span class="text-sm opacity-50">/</span>
			<span class="text-sm font-semibold">Event Setup</span>
		</div>

		{#if editMode}
			<div class="card bg-surface-100-900 p-6 space-y-4 shadow-xl">
				<h2 class="h3">Edit Event</h2>
				<form
					class="space-y-4"
					onsubmit={(e) => {
						e.preventDefault();
						void saveEdit();
					}}
				>
					<div>
						<label class="label text-sm font-semibold" for="edit-title">Title</label>
						<input id="edit-title" class="input" type="text" bind:value={editTitle} />
					</div>
					<div>
						<label class="label text-sm font-semibold" for="edit-desc">Description</label>
						<textarea id="edit-desc" class="textarea" rows="3" bind:value={editDescription}
						></textarea>
					</div>
					<div>
						<label class="label text-sm font-semibold" for="edit-passphrase">Passphrase</label>
						<input id="edit-passphrase" class="input" type="text" bind:value={editPassphrase} />
					</div>
					<div>
						<label class="flex items-center gap-2 cursor-pointer">
							<input type="checkbox" class="checkbox" bind:checked={editParticipantsCanPoll} />
							<span class="text-sm font-semibold">Participants can create polls</span>
						</label>
					</div>
					<div>
						<label class="flex items-center gap-2 cursor-pointer">
							<input type="checkbox" class="checkbox" bind:checked={editParticipantsCanChecklist} />
							<span class="text-sm font-semibold">Participants can add checklist items</span>
						</label>
					</div>
					<div>
						<label class="flex items-center gap-2 cursor-pointer">
							<input
								type="checkbox"
								class="checkbox"
								bind:checked={editParticipantsCanShoppingList}
							/>
							<span class="text-sm font-semibold">Participants can add shopping lists</span>
						</label>
					</div>
					<div class="flex gap-2">
						<button class="btn preset-filled-primary-500" type="submit" disabled={saving}>
							{saving ? 'Saving...' : 'Save'}
						</button>
						<button
							class="btn preset-tonal"
							type="button"
							onclick={() => {
								editMode = false;
							}}
						>
							Cancel
						</button>
					</div>
				</form>
			</div>
		{:else}
			<div class="flex items-start justify-between">
				<EventHeader {event} />
				<button class="btn preset-tonal" onclick={startEdit}>Edit</button>
			</div>
		{/if}

		{#if error}
			<p class="text-error-500">{error}</p>
		{/if}

		<!-- Time block creation -->
		<div class="card bg-surface-100-900 p-6 space-y-4 shadow-xl">
			<h3 class="h4">Add Time Slots</h3>
			<p class="text-sm opacity-75">
				Create time blocks that participants can vote on. Click or paint on the grid to add slots.
			</p>
			<SlotGrid
				mode="setup"
				{eventId}
				{adminToken}
				{timeOptions}
				onOptionsChanged={loadTimeOptions}
			/>
		</div>

		<!-- Open for voting -->
		<div class="card bg-surface-100-900 p-6 space-y-4 shadow-xl">
			<h3 class="h4">Ready to Vote?</h3>
			{#if timeOptions.length === 0}
				<p class="text-sm opacity-75">
					Add at least one time slot above before opening for voting.
				</p>
			{:else}
				<p class="text-sm opacity-75">
					You've added {timeOptions.length} time slot{timeOptions.length === 1 ? '' : 's'}. When
					you're ready, proceed to the voting page where you and participants can vote.
				</p>
			{/if}
			<button
				class="btn preset-filled-primary-500"
				onclick={goToVoting}
				disabled={timeOptions.length === 0}
			>
				Open for Voting
			</button>
		</div>
	{/if}
</div>
