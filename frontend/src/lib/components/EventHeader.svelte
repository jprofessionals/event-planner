<script lang="ts">
	import type { EventResponse } from '$lib/api/client';

	let { event, isAdmin = false }: { event: EventResponse; isAdmin?: boolean } = $props();
</script>

<header class="space-y-2 mb-6">
	<div class="flex items-center gap-3">
		<h1 class="h1">{event.title}</h1>
		{#if isAdmin}
			<span class="badge preset-filled-warning-500">Admin</span>
		{/if}
	</div>
	{#if event.description}
		<p class="text-lg opacity-75">{event.description}</p>
	{/if}
	{#if event.decidedTimeStart}
		<div class="card bg-surface-100-900 p-4 mt-4">
			<p class="text-lg font-bold">
				Decided time: {new Date(event.decidedTimeStart).toLocaleString()}
				{#if event.decidedTimeEnd}
					&mdash; {new Date(event.decidedTimeEnd).toLocaleString()}
				{/if}
			</p>
		</div>
	{/if}
</header>
