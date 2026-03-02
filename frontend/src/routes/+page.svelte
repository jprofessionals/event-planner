<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { events, ApiError } from '$lib/api/client';
	import { auth } from '$lib/stores/auth';
	import { get } from 'svelte/store';
	import { onMount } from 'svelte';
	import { generatePassphrase } from '$lib/passphrase';

	let showForm = $state(page.url.searchParams.has('create'));
	let title = $state('');
	let description = $state('');
	let secretVotes = $state(false);
	let participantsCanPoll = $state(true);
	let participantsCanChecklist = $state(true);
	let participantsCanShoppingList = $state(true);
	let passphrase = $state(generatePassphrase());
	let loading = $state(false);
	let error = $state('');

	// Redirect logged-in users to dashboard
	onMount(() => {
		const authState = get(auth);
		if (authState.token && !page.url.searchParams.has('create')) {
			void goto(resolve('/dashboard'));
		}
	});

	async function createEvent(e: Event) {
		e.preventDefault();
		if (!title.trim() || !passphrase.trim()) return;

		loading = true;
		error = '';
		try {
			const authState = get(auth);
			const event = await events.create(
				{
					title: title.trim(),
					description: description.trim() || undefined,
					secretVotes,
					participantsCanPoll,
					participantsCanChecklist,
					participantsCanShoppingList,
					passphrase: passphrase.trim(),
				},
				authState.token ?? undefined,
			);
			const adminToken = event.adminToken;
			// eslint-disable-next-line svelte/no-navigation-without-resolve -- resolve used with hash fragment appended
			void goto(`${resolve('/event/[eventId]/admin', { eventId: event.id })}#key=${adminToken}`);
		} catch (err) {
			if (err instanceof ApiError) {
				error = `Failed to create event: ${err.body}`;
			} else {
				error = 'Failed to create event. Please try again.';
			}
		} finally {
			loading = false;
		}
	}
</script>

<div class="container mx-auto max-w-2xl p-8 space-y-8">
	<header class="text-center space-y-4">
		<h1 class="h1">Event Planner</h1>
		<p class="text-lg opacity-75">
			Plan your next event together. Find a time, vote on decisions, and coordinate as a group.
		</p>
		{#if !showForm}
			<button
				class="btn preset-filled-primary-500 text-lg px-8 py-3"
				onclick={() => {
					showForm = true;
				}}
			>
				Create Event
			</button>
		{/if}
	</header>

	{#if showForm}
		<div class="card bg-surface-100-900 p-6 space-y-6 shadow-xl">
			<h2 class="h3">Create a New Event</h2>

			<form class="space-y-4" onsubmit={createEvent}>
				<div>
					<label class="label text-sm font-semibold" for="title">Event Title</label>
					<input
						id="title"
						class="input"
						type="text"
						placeholder="e.g., Team Offsite Planning"
						bind:value={title}
						required
					/>
				</div>

				<div>
					<label class="label text-sm font-semibold" for="description">Description (optional)</label
					>
					<textarea
						id="description"
						class="textarea"
						rows="3"
						placeholder="What is this event about?"
						bind:value={description}
					></textarea>
				</div>

				<div>
					<label class="flex items-center gap-2 cursor-pointer">
						<input type="checkbox" class="checkbox" bind:checked={secretVotes} />
						<span class="text-sm font-semibold">Secret votes</span>
					</label>
					<p class="text-sm opacity-50 mt-1">
						When enabled, participants can only see their own votes. The organizer can always see
						all votes.
					</p>
				</div>

				<div>
					<label class="flex items-center gap-2 cursor-pointer">
						<input type="checkbox" class="checkbox" bind:checked={participantsCanPoll} />
						<span class="text-sm font-semibold">Participants can create polls</span>
					</label>
					<p class="text-sm opacity-50 mt-1">When disabled, only the organizer can create polls.</p>
				</div>

				<div>
					<label class="flex items-center gap-2 cursor-pointer">
						<input type="checkbox" class="checkbox" bind:checked={participantsCanChecklist} />
						<span class="text-sm font-semibold">Participants can add checklist items</span>
					</label>
					<p class="text-sm opacity-50 mt-1">
						When disabled, only the organizer can add checklist items.
					</p>
				</div>

				<div>
					<label class="flex items-center gap-2 cursor-pointer">
						<input type="checkbox" class="checkbox" bind:checked={participantsCanShoppingList} />
						<span class="text-sm font-semibold">Participants can add shopping lists</span>
					</label>
					<p class="text-sm opacity-50 mt-1">
						When disabled, only the organizer can add shopping lists.
					</p>
				</div>

				<div>
					<label class="label text-sm font-semibold" for="passphrase">Passphrase</label>
					<input
						id="passphrase"
						class="input"
						type="text"
						placeholder="Shared secret for participants to join"
						bind:value={passphrase}
						required
					/>
					<p class="text-sm opacity-50 mt-1">
						Participants will need this passphrase to join the event.
					</p>
				</div>

				{#if error}
					<p class="text-error-500">{error}</p>
				{/if}

				<button
					class="btn preset-filled-primary-500 w-full"
					type="submit"
					disabled={loading || !title.trim() || !passphrase.trim()}
				>
					{loading ? 'Creating...' : 'Create Event'}
				</button>
			</form>
		</div>
	{/if}

	<div class="flex justify-center gap-4">
		{#if $auth.token}
			<a href={resolve('/dashboard')} class="btn preset-tonal">Dashboard</a>
		{:else}
			<a href={resolve('/login')} class="btn preset-tonal">Log In</a>
			<a href={resolve('/register')} class="btn preset-tonal">Sign Up</a>
		{/if}
	</div>
</div>
