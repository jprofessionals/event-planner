<script lang="ts">
	import { checklist, type ChecklistItemResponse } from '$lib/api/client';

	let {
		eventId,
		canAdd = true,
		adminToken,
		onUpdated,
	}: {
		eventId: string;
		canAdd?: boolean;
		adminToken?: string;
		onUpdated?: () => void;
	} = $props();

	let items = $state<ChecklistItemResponse[]>([]);
	let loading = $state(true);
	let error = $state('');
	let newText = $state('');
	let newAssignedTo = $state('');
	let adding = $state(false);

	async function loadItems() {
		loading = true;
		try {
			items = await checklist.list(eventId);
		} catch {
			error = 'Failed to load checklist';
		} finally {
			loading = false;
		}
	}

	async function addItem() {
		if (!newText.trim()) return;
		adding = true;
		error = '';
		try {
			const item = await checklist.add(
				eventId,
				{
					text: newText.trim(),
					assignedTo: newAssignedTo.trim() || undefined,
				},
				adminToken,
			);
			items = [...items, item];
			newText = '';
			newAssignedTo = '';
			onUpdated?.();
		} catch {
			error = 'Failed to add item';
		} finally {
			adding = false;
		}
	}

	async function toggleItem(item: ChecklistItemResponse) {
		try {
			const updated = await checklist.update(eventId, item.id, {
				completed: !item.completed,
			});
			items = items.map((i) => (i.id === updated.id ? updated : i));
			onUpdated?.();
		} catch {
			error = 'Failed to update item';
		}
	}

	export function refresh() {
		void loadItems();
	}

	$effect(() => {
		void loadItems();
	});
</script>

<div class="space-y-4">
	<h2 class="h3">Checklist</h2>

	{#if loading}
		<p class="opacity-75">Loading checklist...</p>
	{:else}
		<div class="space-y-2">
			{#each items as item (item.id)}
				<label
					class="flex items-center gap-3 p-2 rounded-lg hover:bg-surface-100-900 cursor-pointer"
				>
					<input
						type="checkbox"
						class="checkbox"
						checked={item.completed}
						onchange={() => toggleItem(item)}
						aria-label="Mark '{item.text}' as {item.completed ? 'incomplete' : 'complete'}"
					/>
					<span class={item.completed ? 'line-through opacity-50' : ''}>
						{item.text}
					</span>
					{#if item.assignedTo}
						<span class="badge preset-tonal text-xs ml-auto">{item.assignedTo}</span>
					{/if}
				</label>
			{/each}
		</div>

		{#if items.length === 0}
			<p class="opacity-75 text-sm">No items yet. Add one below.</p>
		{/if}

		{#if error}
			<p class="text-error-500">{error}</p>
		{/if}

		{#if canAdd}
			<form
				class="flex gap-2 items-end"
				onsubmit={(e) => {
					e.preventDefault();
					void addItem();
				}}
			>
				<div class="flex-1">
					<label class="label text-sm" for="checklist-text">Item</label>
					<input
						id="checklist-text"
						class="input"
						type="text"
						placeholder="What needs to be done?"
						bind:value={newText}
					/>
				</div>
				<div class="w-40">
					<label class="label text-sm" for="checklist-assigned">Assigned to</label>
					<input
						id="checklist-assigned"
						class="input"
						type="text"
						placeholder="(optional)"
						bind:value={newAssignedTo}
					/>
				</div>
				<button
					class="btn preset-filled-primary-500"
					type="submit"
					disabled={adding || !newText.trim()}
				>
					{adding ? 'Adding...' : 'Add'}
				</button>
			</form>
		{/if}
	{/if}
</div>
