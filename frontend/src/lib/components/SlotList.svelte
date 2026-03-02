<script lang="ts">
	/* eslint-disable svelte/prefer-svelte-reactivity -- Map used as non-reactive local computation in $derived */
	import { scheduling, type TimeOptionResponse } from '$lib/api/client';

	let {
		eventId,
		participantName,
		timeOptions,
		secretVotes,
		onVoteCast,
	}: {
		eventId: string;
		participantName: string;
		timeOptions: TimeOptionResponse[];
		secretVotes: boolean;
		onVoteCast?: () => void;
	} = $props();

	let error = $state('');
	let working = $state(false);

	// Group time options by date
	const groupedByDate = $derived.by(() => {
		const groups = new Map<string, TimeOptionResponse[]>();
		const sorted = [...timeOptions].sort(
			(a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime(),
		);
		for (const opt of sorted) {
			const d = new Date(opt.startTime);
			const dateKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
			if (!groups.has(dateKey)) {
				groups.set(dateKey, []);
			}
			groups.get(dateKey)!.push(opt);
		}
		return groups;
	});

	function formatDateHeader(dateKey: string): string {
		const [year, month, day] = dateKey.split('-').map(Number);
		const d = new Date(year, month - 1, day);
		return d.toLocaleDateString(undefined, {
			weekday: 'long',
			month: 'long',
			day: 'numeric',
			year: 'numeric',
		});
	}

	function formatTimeRange(opt: TimeOptionResponse): string {
		const start = new Date(opt.startTime);
		const end = new Date(opt.endTime);
		const fmt = (d: Date) =>
			d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
		return `${fmt(start)} – ${fmt(end)}`;
	}

	function getMyVote(option: TimeOptionResponse): string | null {
		const v = option.votes.find((v) => v.participantName === participantName);
		return v ? v.vote : null;
	}

	async function castVote(option: TimeOptionResponse, vote: string) {
		const currentVote = getMyVote(option);
		// Clicking same vote again removes it
		const newVote = currentVote === vote ? 'NONE' : vote;

		working = true;
		error = '';
		try {
			await scheduling.castVotes(eventId, {
				participantName,
				votes: [{ timeOptionId: option.id, vote: newVote }],
			});
			onVoteCast?.();
		} catch {
			error = 'Failed to cast vote';
		} finally {
			working = false;
		}
	}
</script>

<div class="space-y-6">
	{#if timeOptions.length === 0}
		<p class="opacity-75">No time options have been added yet.</p>
	{:else}
		{#each [...groupedByDate] as [dateKey, options] (dateKey)}
			<div class="space-y-3">
				<h3 class="h4">{formatDateHeader(dateKey)}</h3>
				{#each options as option (option.id)}
					{@const myVote = getMyVote(option)}
					<div class="card bg-surface-100-900 p-4 space-y-2">
						<p class="font-semibold">{formatTimeRange(option)}</p>

						<div class="flex gap-2">
							<button
								class="btn {myVote === 'YES' ? 'preset-filled-success-500' : 'preset-tonal'}"
								onclick={() => castVote(option, 'YES')}
								disabled={working}
							>
								Yes
							</button>
							<button
								class="btn {myVote === 'MAYBE' ? 'preset-filled-warning-500' : 'preset-tonal'}"
								onclick={() => castVote(option, 'MAYBE')}
								disabled={working}
							>
								Maybe
							</button>
							<button
								class="btn {myVote === 'NO' ? 'preset-filled-error-500' : 'preset-tonal'}"
								onclick={() => castVote(option, 'NO')}
								disabled={working}
							>
								No
							</button>
						</div>

						{#if !secretVotes && option.votes.length > 0}
							<div class="text-sm opacity-75 space-y-1 mt-2 flex flex-wrap gap-1">
								{#each option.votes as v (v.id)}
									<span
										class="badge {v.vote === 'YES'
											? 'preset-filled-success-500'
											: v.vote === 'MAYBE'
												? 'preset-filled-warning-500'
												: v.vote === 'NO'
													? 'preset-filled-error-500'
													: 'preset-tonal'}"
									>
										{v.participantName}: {v.vote}
									</span>
								{/each}
							</div>
						{:else if secretVotes && myVote && myVote !== 'NONE'}
							<div class="text-sm opacity-75 mt-2">
								<span
									class="badge {myVote === 'YES'
										? 'preset-filled-success-500'
										: myVote === 'MAYBE'
											? 'preset-filled-warning-500'
											: 'preset-filled-error-500'}"
								>
									Your vote: {myVote}
								</span>
							</div>
						{/if}
					</div>
				{/each}
			</div>
		{/each}
	{/if}

	{#if error}
		<p class="text-error-500">{error}</p>
	{/if}
</div>
