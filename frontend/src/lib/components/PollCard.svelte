<script lang="ts">
	import { polls, type PollResponse } from '$lib/api/client';

	let {
		poll,
		eventId,
		participantName,
		onVoted,
	}: {
		poll: PollResponse;
		eventId: string;
		participantName: string;
		onVoted?: () => void;
	} = $props();

	let selectedOptions = $state<Set<string>>(new Set());
	let submitting = $state(false);
	let error = $state('');
	let voted = $state(false);

	function toggleOption(optionId: string) {
		if (voted) return;
		// eslint-disable-next-line svelte/prefer-svelte-reactivity -- non-reactive local computation
		const newSet = new Set(selectedOptions);
		if (poll.allowMultiple) {
			if (newSet.has(optionId)) {
				newSet.delete(optionId);
			} else {
				newSet.add(optionId);
			}
		} else {
			newSet.clear();
			newSet.add(optionId);
		}
		selectedOptions = newSet;
	}

	async function submitVote() {
		if (selectedOptions.size === 0) return;
		submitting = true;
		error = '';
		try {
			await polls.vote(eventId, poll.id, {
				participantName,
				optionIds: Array.from(selectedOptions),
			});
			voted = true;
			onVoted?.();
		} catch {
			error = 'Failed to submit vote';
		} finally {
			submitting = false;
		}
	}

	const totalVotes = $derived(poll.options.reduce((sum, o) => sum + o.voteCount, 0));
</script>

<div class="card bg-surface-100-900 p-4 space-y-4 shadow-xl">
	<h3 class="h4">{poll.question}</h3>
	<p class="text-sm opacity-75">
		{poll.allowMultiple ? 'Select multiple options' : 'Select one option'}
	</p>

	<div class="space-y-2">
		{#each poll.options as option (option.id)}
			{@const pct = totalVotes > 0 ? Math.round((option.voteCount / totalVotes) * 100) : 0}
			<button
				class="w-full text-left p-3 rounded-lg border transition-all
                    {selectedOptions.has(option.id)
					? 'border-primary-500 bg-primary-500/10'
					: 'border-surface-300-700 hover:border-surface-400-600'}"
				onclick={() => toggleOption(option.id)}
				disabled={voted}
				aria-pressed={selectedOptions.has(option.id)}
				aria-label="{option.text}: {option.voteCount} votes ({pct}%)"
			>
				<div class="flex justify-between items-center mb-1">
					<span>{option.text}</span>
					<span class="text-sm opacity-75">{option.voteCount} votes ({pct}%)</span>
				</div>
				<div class="w-full bg-surface-200-800 rounded-full h-2">
					<div class="bg-primary-500 h-2 rounded-full transition-all" style="width: {pct}%"></div>
				</div>
			</button>
		{/each}
	</div>

	{#if error}
		<p class="text-error-500">{error}</p>
	{/if}

	{#if !voted}
		<button
			class="btn preset-filled-primary-500"
			onclick={submitVote}
			disabled={submitting || selectedOptions.size === 0}
		>
			{submitting ? 'Voting...' : 'Vote'}
		</button>
	{:else}
		<p class="text-sm text-success-500">Vote submitted!</p>
	{/if}
</div>
