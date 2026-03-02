<script lang="ts">
	import { shoppingLists, type ShoppingListResponse } from '$lib/api/client';
	import { PUBLIC_SHOPPING_LIST_API_URL } from '$env/static/public';
	import { auth } from '$lib/stores/auth';
	import { get } from 'svelte/store';

	let {
		eventId,
		adminToken = undefined,
		participantsCanShoppingList = false,
	}: {
		eventId: string;
		adminToken?: string;
		participantsCanShoppingList: boolean;
	} = $props();

	const shoppingListApiUrl = PUBLIC_SHOPPING_LIST_API_URL ?? '';

	let lists = $state<ShoppingListResponse[]>([]);
	let showAddForm = $state(false);
	let newTitle = $state('');
	let newEmail = $state(get(auth).user?.email ?? '');
	let loading = $state(true);
	let error = $state('');
	let creating = $state(false);
	let widgetScriptLoaded = $state(false);
	let widgetScriptFailed = $state(false);

	const canCreate = $derived(adminToken != null || participantsCanShoppingList);

	async function loadLists() {
		try {
			lists = await shoppingLists.list(eventId);
		} catch {
			error = 'Failed to load shopping lists';
		} finally {
			loading = false;
		}
	}

	function loadWidgetScript() {
		if (!shoppingListApiUrl) return;
		if (document.querySelector('script[data-shopping-list-widget]')) {
			widgetScriptLoaded = true;
			return;
		}
		const script = document.createElement('script');
		script.src = `${shoppingListApiUrl}/widget.js`;
		script.setAttribute('data-shopping-list-widget', 'true');
		script.onload = () => {
			widgetScriptLoaded = true;
		};
		script.onerror = () => {
			widgetScriptFailed = true;
		};
		document.head.appendChild(script);
	}

	async function addShoppingList() {
		if (!newTitle.trim()) return;
		creating = true;
		error = '';
		try {
			const list = await shoppingLists.create(
				eventId,
				{ title: newTitle.trim(), email: newEmail.trim() || undefined },
				adminToken,
			);
			lists = [...lists, list];
			newTitle = '';
			newEmail = '';
			showAddForm = false;
		} catch {
			error = 'Failed to create shopping list';
		} finally {
			creating = false;
		}
	}

	async function removeList(listId: string) {
		if (!adminToken) return;
		error = '';
		try {
			await shoppingLists.remove(eventId, listId, adminToken);
			lists = lists.filter((l) => l.id !== listId);
		} catch {
			error = 'Failed to remove shopping list';
		}
	}

	export function refresh() {
		void loadLists();
	}

	function mountWidget(container: Element, getAttrs: () => Record<string, string>) {
		$effect(() => {
			const attrs = getAttrs();
			const token = attrs.token ?? '';
			const apiUrl = attrs['api-url'] ?? '';
			container.textContent = '';
			const widget = document.createElement('shopping-list-widget');
			widget.setAttribute('token', token);
			widget.setAttribute('api-url', apiUrl);
			container.appendChild(widget);
		});
	}

	$effect(() => {
		void loadLists();
		loadWidgetScript();
	});
</script>

<div class="space-y-4">
	<h2 class="h3">Shopping Lists</h2>

	{#if loading}
		<p class="opacity-75">Loading shopping lists...</p>
	{:else}
		{#if error}
			<p class="text-error-500">{error}</p>
		{/if}

		{#if lists.length === 0}
			<p class="opacity-75 text-sm">No shopping lists yet.</p>
		{:else}
			<div class="space-y-4">
				{#each lists as list (list.id)}
					<div class="card bg-surface-100-900 p-4 shadow">
						<div class="flex items-center justify-between mb-2">
							<h3 class="font-medium">{list.title}</h3>
							<div class="flex items-center gap-2">
								<span class="text-sm opacity-50">by {list.createdByParticipant}</span>
								{#if adminToken}
									<button
										class="btn btn-sm preset-tonal text-error-500"
										onclick={() => {
											if (confirm(`Remove shopping list "${list.title}"?`)) {
												void removeList(list.id);
											}
										}}
									>
										Remove
									</button>
								{/if}
							</div>
						</div>
						{#if widgetScriptLoaded}
							<div
								use:mountWidget={() => ({ token: list.shareToken, 'api-url': shoppingListApiUrl })}
							></div>
						{:else if !widgetScriptFailed && shoppingListApiUrl}
							<p class="opacity-50 text-sm">Loading widget...</p>
						{:else}
							<button
								class="btn btn-sm preset-tonal"
								onclick={() => window.open(list.widgetUrl, '_blank', 'noopener,noreferrer')}
							>
								Open Shopping List
							</button>
						{/if}
					</div>
				{/each}
			</div>
		{/if}

		{#if showAddForm && canCreate}
			<div class="card bg-surface-100-900 p-4 space-y-3 shadow">
				<h4 class="h5">New Shopping List</h4>
				<form
					class="space-y-3"
					onsubmit={(e) => {
						e.preventDefault();
						void addShoppingList();
					}}
				>
					<div>
						<label class="label text-sm font-semibold" for="sl-title">Title</label>
						<input
							id="sl-title"
							class="input"
							type="text"
							bind:value={newTitle}
							placeholder="e.g. Party supplies"
						/>
					</div>
					<div>
						<label class="label text-sm font-semibold" for="sl-email">Email (optional)</label>
						<input
							id="sl-email"
							class="input"
							type="email"
							bind:value={newEmail}
							placeholder="Associate with an account"
						/>
					</div>
					<div class="flex gap-2">
						<button
							class="btn btn-sm preset-filled-primary-500"
							type="submit"
							disabled={creating || !newTitle.trim()}
						>
							{creating ? 'Creating...' : 'Create'}
						</button>
						<button
							class="btn btn-sm preset-tonal"
							type="button"
							onclick={() => {
								showAddForm = false;
							}}
						>
							Cancel
						</button>
					</div>
				</form>
			</div>
		{:else if canCreate}
			<button
				class="btn preset-tonal w-full"
				onclick={() => {
					showAddForm = true;
				}}
			>
				Add Shopping List
			</button>
		{/if}
	{/if}
</div>
