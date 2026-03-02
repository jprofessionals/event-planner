<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { untrack } from 'svelte';
	import {
		events,
		scheduling,
		polls as pollsApi,
		ApiError,
		type EventResponse,
		type PollResponse,
		type TimeOptionResponse,
	} from '$lib/api/client';
	import { getParticipantSession, getAdminToken, setAdminToken } from '$lib/stores/event';
	import { auth } from '$lib/stores/auth';
	import { get } from 'svelte/store';
	import { subscribeToEvent } from '$lib/api/sse';
	import EventHeader from '$lib/components/EventHeader.svelte';
	import SlotGrid from '$lib/components/SlotGrid.svelte';
	import PollCard from '$lib/components/PollCard.svelte';
	import Checklist from '$lib/components/Checklist.svelte';
	import CommentThread from '$lib/components/CommentThread.svelte';
	import ShoppingLists from '$lib/components/ShoppingLists.svelte';
	import { Tabs } from '@skeletonlabs/skeleton-svelte';

	const eventId = $derived(page.params.eventId!);

	let event = $state<EventResponse | null>(null);
	let pollList = $state<PollResponse[]>([]);
	let loading = $state(true);
	let error = $state('');
	let activeTab = $state('polls');
	let participantName = $state('');
	let timeOptions = $state<TimeOptionResponse[]>([]);
	let viewMode = $state<'vote' | 'overview' | 'edit'>('vote');
	let adminToken = $state<string | null>(null);
	let deciding = $state(false);
	let pendingDecision = $state<TimeOptionResponse | null>(null);
	let unsubscribe: (() => void) | null = null;

	// Component refs for SSE refresh
	let checklistRef = $state<{ refresh: () => void } | undefined>();
	let commentThreadRef = $state<{ refresh: () => void } | undefined>();
	let shoppingListsRef = $state<{ refresh: () => void } | undefined>();

	// Admin name prompt
	let needsAdminName = $state(false);
	let adminNameInput = $state('');

	// Tab notification state
	let newPolls = $state(false);
	let newChecklist = $state(false);
	let newComments = $state(false);
	let newShoppingLists = $state(false);

	// Copy share link state
	let copied = $state(false);
	let copiedPassphrase = $state(false);

	// Edit event state (admin)
	let editMode = $state(false);
	let editTitle = $state('');
	let editDescription = $state('');
	let saving = $state(false);

	// Create poll state
	let showCreatePoll = $state(false);
	let pollQuestion = $state('');
	let pollOptions = $state('');
	let pollAllowMultiple = $state(false);
	let creatingPoll = $state(false);

	const isAdmin = $derived(!!adminToken);
	const canCreatePolls = $derived(isAdmin || (event?.participantsCanPoll ?? true));
	const canAddChecklistItems = $derived(isAdmin || (event?.participantsCanChecklist ?? true));
	const canAddShoppingLists = $derived(isAdmin || (event?.participantsCanShoppingList ?? false));

	async function loadTimeOptions() {
		try {
			const headers: { participantName?: string; adminToken?: string } = {};
			if (participantName) headers.participantName = participantName;
			if (adminToken) headers.adminToken = adminToken;
			timeOptions = await scheduling.getTimeOptions(eventId, headers);
		} catch {
			// Silently fail
		}
	}

	async function loadEvent() {
		const isInitialLoad = !event;
		if (isInitialLoad) loading = true;
		try {
			event = await events.get(eventId, adminToken ?? undefined);
			if (event.stage === 'SCHEDULING') {
				await loadTimeOptions();
			} else if (event.stage === 'PLANNING') {
				await loadPolls();
			}
		} catch {
			error = 'Failed to load event.';
		} finally {
			if (isInitialLoad) loading = false;
		}
	}

	async function loadPolls() {
		try {
			pollList = await pollsApi.list(eventId);
		} catch {
			// Silently fail
		}
	}

	function setupSSE() {
		unsubscribe = subscribeToEvent(eventId, (type) => {
			if (type === 'event-updated' || type === 'time-decided') {
				void loadEvent();
			} else if (
				type === 'votes-cast' ||
				type === 'time-options-added' ||
				type === 'time-option-deleted'
			) {
				void loadTimeOptions();
				void loadEvent();
			} else if (type === 'poll-created' || type === 'poll-vote') {
				void loadPolls();
				if (activeTab !== 'polls') newPolls = true;
			} else if (type === 'checklist-item-added' || type === 'checklist-item-updated') {
				checklistRef?.refresh();
				if (activeTab !== 'checklist') newChecklist = true;
			} else if (type === 'comment-added') {
				commentThreadRef?.refresh();
				if (activeTab !== 'comments') newComments = true;
			} else if (type === 'shopping-list-added' || type === 'shopping-list-removed') {
				shoppingListsRef?.refresh();
				if (activeTab !== 'shopping') newShoppingLists = true;
			} else {
				void loadEvent();
			}
		});
	}

	function handleDecide(option: TimeOptionResponse) {
		if (!adminToken) return;
		pendingDecision = option;
	}

	async function confirmDecision() {
		if (!adminToken || !pendingDecision) return;
		deciding = true;
		error = '';
		const option = pendingDecision;
		pendingDecision = null;
		try {
			await events.decide(
				eventId,
				{ startTime: option.startTime, endTime: option.endTime },
				adminToken,
			);
			await loadEvent();
		} catch (err) {
			if (err instanceof ApiError) {
				error = `Failed to decide time: ${err.body}`;
			} else {
				error = 'Failed to decide time.';
			}
		} finally {
			deciding = false;
		}
	}

	function cancelDecision() {
		pendingDecision = null;
	}

	function formatSlotTime(iso: string): string {
		return new Date(iso).toLocaleString(undefined, {
			weekday: 'short',
			month: 'short',
			day: 'numeric',
			hour: '2-digit',
			minute: '2-digit',
		});
	}

	async function copyShareLink() {
		const link = `${page.url.origin}/event/${eventId}/join`;
		await navigator.clipboard.writeText(link);
		copied = true;
		setTimeout(() => {
			copied = false;
		}, 2000);
	}

	async function copyPassphrase() {
		if (!event?.passphrase) return;
		await navigator.clipboard.writeText(event.passphrase);
		copiedPassphrase = true;
		setTimeout(() => {
			copiedPassphrase = false;
		}, 2000);
	}

	function startEdit() {
		if (!event) return;
		editTitle = event.title;
		editDescription = event.description || '';
		editMode = true;
	}

	async function saveEdit() {
		if (!adminToken || !event) return;
		saving = true;
		error = '';
		try {
			event = await events.update(
				eventId,
				{
					title: editTitle.trim() || undefined,
					description: editDescription.trim() || undefined,
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

	async function createPoll() {
		if (!pollQuestion.trim() || !pollOptions.trim()) return;
		creatingPoll = true;
		error = '';
		try {
			const options = pollOptions
				.split('\n')
				.map((o) => o.trim())
				.filter((o) => o.length > 0);
			await pollsApi.create(
				eventId,
				{
					question: pollQuestion.trim(),
					options,
					allowMultiple: pollAllowMultiple,
				},
				adminToken ?? undefined,
			);
			pollQuestion = '';
			pollOptions = '';
			pollAllowMultiple = false;
			showCreatePoll = false;
			await loadPolls();
		} catch (err) {
			if (err instanceof ApiError) {
				error = `Failed to create poll: ${err.body}`;
			} else {
				error = 'Failed to create poll.';
			}
		} finally {
			creatingPoll = false;
		}
	}

	function submitAdminName() {
		const name = adminNameInput.trim();
		if (!name) return;
		sessionStorage.setItem(`admin-name:${eventId}`, name);
		participantName = name;
		needsAdminName = false;
		loading = true;
		void loadEvent();
		unsubscribe?.();
		setupSSE();
	}

	$effect(() => {
		// Check for admin token in URL hash fragment
		const hash = typeof window !== 'undefined' ? window.location.hash : '';
		const hashKey = hash.startsWith('#key=') ? hash.slice(5) : null;
		if (hashKey) {
			setAdminToken(eventId, hashKey);
			adminToken = hashKey;
			history.replaceState({}, '', window.location.pathname);
		} else {
			adminToken = getAdminToken(eventId);
		}

		// Get participant name from session or stored admin name
		const session = getParticipantSession(eventId);
		if (session) {
			participantName = session.displayName;
		} else if (adminToken) {
			const authState = get(auth);
			const storedName =
				typeof sessionStorage !== 'undefined'
					? sessionStorage.getItem(`admin-name:${eventId}`)
					: null;
			if (authState.user?.displayName) {
				participantName = authState.user.displayName;
			} else if (storedName) {
				participantName = storedName;
			} else {
				needsAdminName = true;
				loading = false;
				return;
			}
		} else {
			void goto(resolve('/event/[eventId]/join', { eventId }));
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
	{#if needsAdminName}
		<div class="card bg-surface-100-900 p-6 space-y-4 shadow-xl max-w-md mx-auto">
			<h2 class="h3">Enter Your Name</h2>
			<p class="text-sm opacity-75">
				Choose a display name for voting. Other participants will see this name.
			</p>
			<form
				class="space-y-4"
				onsubmit={(e) => {
					e.preventDefault();
					submitAdminName();
				}}
			>
				<div>
					<label class="label text-sm font-semibold" for="admin-name">Display Name</label>
					<input
						id="admin-name"
						class="input"
						type="text"
						placeholder="Your name"
						bind:value={adminNameInput}
						required
					/>
				</div>
				<button
					class="btn preset-filled-primary-500 w-full"
					type="submit"
					disabled={!adminNameInput.trim()}
				>
					Continue
				</button>
			</form>
		</div>
	{:else if loading}
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
			<span>Loading event...</span>
		</div>
	{:else if error && !event}
		<div class="text-center space-y-4">
			<h1 class="h2">Error</h1>
			<p class="text-error-500">{error}</p>
			<a href={resolve('/')} class="btn preset-tonal">Go Home</a>
		</div>
	{:else if event}
		<div class="flex items-center gap-2 mb-2">
			<a href={resolve('/')} class="btn btn-sm preset-tonal">Home</a>
			{#if isAdmin}
				<span class="text-sm opacity-50">/</span>
				<a href={resolve('/event/[eventId]/admin', { eventId })} class="btn btn-sm preset-tonal">
					Event Setup
				</a>
			{/if}
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
				<EventHeader {event} {isAdmin} />
				{#if isAdmin}
					<button class="btn btn-sm preset-tonal" onclick={startEdit}>Edit</button>
				{/if}
			</div>
		{/if}

		{#if error}
			<p class="text-error-500">{error}</p>
		{/if}

		{#if event.stage === 'SCHEDULING'}
			<!-- View mode toggle -->
			<div class="flex gap-2">
				<button
					class="btn {viewMode === 'vote' ? 'preset-filled-primary-500' : 'preset-tonal'}"
					onclick={() => (viewMode = 'vote')}
				>
					Vote
				</button>
				<button
					class="btn {viewMode === 'overview' ? 'preset-filled-primary-500' : 'preset-tonal'}"
					onclick={() => (viewMode = 'overview')}
				>
					Overview
				</button>
				{#if isAdmin}
					<button
						class="btn {viewMode === 'edit' ? 'preset-filled-primary-500' : 'preset-tonal'}"
						onclick={() => (viewMode = 'edit')}
					>
						Edit Slots
					</button>
				{/if}
			</div>

			{#if deciding}
				<p class="opacity-75">Deciding time...</p>
			{/if}

			{#if pendingDecision}
				<div class="card bg-surface-100-900 p-6 space-y-4 shadow-xl border-2 border-warning-500">
					<h3 class="h4">Confirm Final Date</h3>
					<p>Are you sure you want to pick this time slot as the final date?</p>
					<div class="card bg-surface-200-800 p-3">
						<p class="font-semibold">{formatSlotTime(pendingDecision.startTime)}</p>
						<p class="text-sm opacity-75">to {formatSlotTime(pendingDecision.endTime)}</p>
					</div>
					<div class="flex gap-2">
						<button class="btn preset-filled-primary-500" onclick={confirmDecision}>
							Confirm
						</button>
						<button class="btn preset-tonal" onclick={cancelDecision}> Cancel </button>
					</div>
				</div>
			{/if}

			<SlotGrid
				mode={viewMode}
				{eventId}
				{participantName}
				{timeOptions}
				adminToken={isAdmin ? (adminToken ?? undefined) : undefined}
				onVoteCast={loadTimeOptions}
				onOptionsChanged={loadTimeOptions}
				onDecide={handleDecide}
			/>
		{:else if event.stage === 'PLANNING'}
			<Tabs
				value={activeTab}
				onValueChange={(e: { value: string }) => {
					activeTab = e.value;
					if (e.value === 'polls') newPolls = false;
					if (e.value === 'checklist') newChecklist = false;
					if (e.value === 'comments') newComments = false;
					if (e.value === 'shopping') newShoppingLists = false;
				}}
			>
				<Tabs.List>
					<Tabs.Trigger value="polls">
						<span class="relative">
							Polls
							{#if newPolls}
								<span class="absolute -top-1 -right-3 w-2 h-2 rounded-full bg-red-500"></span>
							{/if}
						</span>
					</Tabs.Trigger>
					<Tabs.Trigger value="checklist">
						<span class="relative">
							Checklist
							{#if newChecklist}
								<span class="absolute -top-1 -right-3 w-2 h-2 rounded-full bg-red-500"></span>
							{/if}
						</span>
					</Tabs.Trigger>
					<Tabs.Trigger value="comments">
						<span class="relative">
							Comments
							{#if newComments}
								<span class="absolute -top-1 -right-3 w-2 h-2 rounded-full bg-red-500"></span>
							{/if}
						</span>
					</Tabs.Trigger>
					<Tabs.Trigger value="shopping">
						<span class="relative">
							Shopping
							{#if newShoppingLists}
								<span class="absolute -top-1 -right-3 w-2 h-2 rounded-full bg-red-500"></span>
							{/if}
						</span>
					</Tabs.Trigger>
					<Tabs.Indicator />
				</Tabs.List>
				<Tabs.Content value="polls">
					<div class="space-y-4 mt-4">
						{#if pollList.length === 0}
							<p class="opacity-75">No polls yet.</p>
						{:else}
							{#each pollList as poll (poll.id)}
								<PollCard {poll} {eventId} {participantName} onVoted={loadPolls} />
							{/each}
						{/if}

						{#if showCreatePoll && canCreatePolls}
							<div class="card bg-surface-100-900 p-4 space-y-3 shadow">
								<h4 class="h5">New Poll</h4>
								<form
									class="space-y-3"
									onsubmit={(e) => {
										e.preventDefault();
										void createPoll();
									}}
								>
									<div>
										<label class="label text-sm font-semibold" for="poll-question">Question</label>
										<input
											id="poll-question"
											class="input"
											type="text"
											placeholder="What should we decide?"
											bind:value={pollQuestion}
										/>
									</div>
									<div>
										<label class="label text-sm font-semibold" for="poll-options"
											>Options (one per line)</label
										>
										<textarea
											id="poll-options"
											class="textarea"
											rows="3"
											placeholder="Option A&#10;Option B&#10;Option C"
											bind:value={pollOptions}
										></textarea>
									</div>
									<label class="flex items-center gap-2 cursor-pointer">
										<input type="checkbox" class="checkbox" bind:checked={pollAllowMultiple} />
										<span class="text-sm">Allow multiple selections</span>
									</label>
									<div class="flex gap-2">
										<button
											class="btn btn-sm preset-filled-primary-500"
											type="submit"
											disabled={creatingPoll || !pollQuestion.trim() || !pollOptions.trim()}
										>
											{creatingPoll ? 'Creating...' : 'Create'}
										</button>
										<button
											class="btn btn-sm preset-tonal"
											type="button"
											onclick={() => {
												showCreatePoll = false;
											}}
										>
											Cancel
										</button>
									</div>
								</form>
							</div>
						{:else if canCreatePolls}
							<button
								class="btn preset-tonal w-full"
								onclick={() => {
									showCreatePoll = true;
								}}
							>
								Create Poll
							</button>
						{/if}
					</div>
				</Tabs.Content>
				<Tabs.Content value="checklist">
					<div class="mt-4">
						<Checklist
							bind:this={checklistRef}
							{eventId}
							canAdd={canAddChecklistItems}
							adminToken={adminToken ?? undefined}
						/>
					</div>
				</Tabs.Content>
				<Tabs.Content value="comments">
					<div class="mt-4">
						<CommentThread bind:this={commentThreadRef} {eventId} {participantName} />
					</div>
				</Tabs.Content>
				<Tabs.Content value="shopping">
					<div class="mt-4">
						<ShoppingLists
							bind:this={shoppingListsRef}
							{eventId}
							adminToken={adminToken ?? undefined}
							participantsCanShoppingList={canAddShoppingLists}
						/>
					</div>
				</Tabs.Content>
			</Tabs>
		{/if}

		<!-- Share link (admin only) -->
		{#if isAdmin}
			<div class="card bg-surface-100-900 p-4 space-y-3 shadow-xl">
				<h3 class="h4">Share with Participants</h3>
				<div class="space-y-1">
					<p class="text-sm opacity-75">Join link:</p>
					<div class="flex items-center gap-2">
						<code class="code p-2 block text-sm break-all flex-1">
							{page.url.origin}/event/{eventId}/join
						</code>
						<button
							class="btn btn-sm {copied ? 'preset-filled-success-500' : 'preset-tonal'}"
							onclick={copyShareLink}
						>
							{copied ? 'Copied!' : 'Copy'}
						</button>
					</div>
				</div>
				{#if event.passphrase}
					<div class="space-y-1">
						<p class="text-sm opacity-75">Passphrase:</p>
						<div class="flex items-center gap-2">
							<code class="code p-2 block text-sm flex-1">{event.passphrase}</code>
							<button
								class="btn btn-sm {copiedPassphrase ? 'preset-filled-success-500' : 'preset-tonal'}"
								onclick={copyPassphrase}
							>
								{copiedPassphrase ? 'Copied!' : 'Copy'}
							</button>
						</div>
					</div>
				{/if}
			</div>
		{/if}
	{/if}
</div>
