<script lang="ts">
	/* eslint-disable svelte/prefer-svelte-reactivity -- Date/Map used as non-reactive local computations in $derived */
	import { scheduling, type TimeOptionResponse } from '$lib/api/client';

	let {
		mode = 'vote',
		eventId,
		adminToken,
		participantName,
		timeOptions,
		onOptionsChanged,
		onVoteCast,
		onDecide,
	}: {
		mode?: 'setup' | 'edit' | 'vote' | 'overview';
		eventId: string;
		adminToken?: string;
		participantName?: string;
		timeOptions: TimeOptionResponse[];
		onOptionsChanged?: () => void;
		onVoteCast?: () => void;
		onDecide?: (option: TimeOptionResponse) => void;
	} = $props();

	let weekOffset = $state(0);
	let error = $state('');

	// Setup mode state
	let durationHours = $state(1);
	let durationMinutes = $derived(Math.round(durationHours * 2) * 30 || 60);
	let interactionMode = $state<'stamp' | 'paint'>('stamp');
	let paintStart = $state<{ day: number; row: number } | null>(null);
	let paintEnd = $state<{ day: number; row: number } | null>(null);
	let isPainting = $state(false);

	// Vote popover state
	let votePopoverOption = $state<TimeOptionResponse | null>(null);
	let popoverX = $state(0);
	let popoverY = $state(0);

	// Tooltip state (overview + vote modes)
	let tooltipOption = $state<TimeOptionResponse | null>(null);
	let tooltipX = $state(0);
	let tooltipY = $state(0);

	const ROWS_START = 8;
	const ROWS_END = 22;
	const ROW_MINUTES = 30;
	const TOTAL_ROWS = ((ROWS_END - ROWS_START) * 60) / ROW_MINUTES;

	const durationPresets = [
		{ label: '30m', value: 30 },
		{ label: '1h', value: 60 },
		{ label: '2h', value: 120 },
		{ label: '4h', value: 240 },
		{ label: '8h', value: 480 },
	];

	const rowLabels = $derived(
		Array.from({ length: TOTAL_ROWS }, (_, i) => {
			const totalMinutes = ROWS_START * 60 + i * ROW_MINUTES;
			const h = Math.floor(totalMinutes / 60);
			const m = totalMinutes % 60;
			return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
		}),
	);

	const weekStart = $derived.by(() => {
		const now = new Date();
		const day = now.getDay();
		const diffToMonday = day === 0 ? -6 : 1 - day;
		const monday = new Date(now);
		monday.setDate(now.getDate() + diffToMonday + weekOffset * 7);
		monday.setHours(0, 0, 0, 0);
		return monday;
	});

	const weekEnd = $derived.by(() => {
		const end = new Date(weekStart);
		end.setDate(end.getDate() + 7);
		return end;
	});

	const days = $derived(
		Array.from({ length: 7 }, (_, i) => {
			const d = new Date(weekStart);
			d.setDate(d.getDate() + i);
			return d;
		}),
	);

	const weekLabel = $derived.by(() => {
		const fmt = (d: Date) =>
			d.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
		const sun = new Date(weekStart);
		sun.setDate(sun.getDate() + 6);
		return `${fmt(weekStart)} – ${fmt(sun)}`;
	});

	function dayHeader(d: Date): string {
		return d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' });
	}

	function cellDateTime(dayDate: Date, rowIndex: number): Date {
		const d = new Date(dayDate);
		const totalMinutes = ROWS_START * 60 + rowIndex * ROW_MINUTES;
		d.setHours(Math.floor(totalMinutes / 60), totalMinutes % 60, 0, 0);
		return d;
	}

	const visibleOptions = $derived(
		timeOptions.filter((opt) => {
			const start = new Date(opt.startTime);
			return start >= weekStart && start < weekEnd;
		}),
	);

	const hasOptionsBefore = $derived(timeOptions.some((opt) => new Date(opt.startTime) < weekStart));
	const hasOptionsAfter = $derived(timeOptions.some((opt) => new Date(opt.startTime) >= weekEnd));

	type CellInfo = {
		option: TimeOptionResponse;
		isStart: boolean;
		spanRows: number;
	};

	const cellMap = $derived.by(() => {
		const map = new Map<string, CellInfo>();
		for (const opt of visibleOptions) {
			const start = new Date(opt.startTime);
			const end = new Date(opt.endTime);
			const dayIndex = Math.floor((start.getTime() - weekStart.getTime()) / (24 * 60 * 60 * 1000));
			if (dayIndex < 0 || dayIndex >= 7) continue;

			const startMinutes = start.getHours() * 60 + start.getMinutes();
			const endMinutes = end.getHours() * 60 + end.getMinutes();
			const startRow = (startMinutes - ROWS_START * 60) / ROW_MINUTES;
			const endRow = (endMinutes - ROWS_START * 60) / ROW_MINUTES;
			const spanRows = Math.max(1, endRow - startRow);

			for (let r = startRow; r < endRow && r < TOTAL_ROWS; r++) {
				const key = `${dayIndex}-${r}`;
				map.set(key, {
					option: opt,
					isStart: r === startRow,
					spanRows,
				});
			}
		}
		return map;
	});

	// --- Vote helpers ---
	function getMyVote(option: TimeOptionResponse): string | null {
		if (!participantName) return null;
		const v = option.votes.find((v) => v.participantName === participantName);
		return v ? v.vote : null;
	}

	// --- Color functions ---
	// Overview color: YES=positive, MAYBE=slightly positive, NO=negative
	// Score formula: (yes + 0.5*maybe - no) / total → range [-1, 1]
	// Maps to HSL hue: 0=red, 60=yellow, 120=green
	// MAYBE pulls toward yellow (positive), so 0/2/1 → yellow, not red
	function overviewHsl(option: TimeOptionResponse): string | null {
		const votes = option.votes;
		if (votes.length === 0) return null;
		const yes = votes.filter((v) => v.vote === 'YES').length;
		const maybe = votes.filter((v) => v.vote === 'MAYBE').length;
		const no = votes.filter((v) => v.vote === 'NO').length;
		const score = (yes + maybe * 0.5 - no) / votes.length;
		const hue = Math.round(Math.max(0, Math.min(120, 60 + score * 60)));
		return `hsl(${hue}, 70%, 45%)`;
	}

	function slotColor(): string {
		if (mode === 'setup' || mode === 'edit') return 'bg-primary-500/60';
		return ''; // vote + overview handled by inline style
	}

	function slotInlineStyle(option: TimeOptionResponse): string {
		if (mode === 'vote') {
			const myVote = getMyVote(option);
			if (myVote === 'YES') return 'background-color: var(--vote-yes)';
			if (myVote === 'MAYBE') return 'background-color: var(--vote-maybe)';
			if (myVote === 'NO') return 'background-color: var(--vote-no)';
			return 'background-color: var(--vote-none); opacity: 0.4';
		}
		if (mode === 'overview') {
			const color = overviewHsl(option);
			if (!color) return 'background-color: hsl(0, 0%, 60%)';
			return `background-color: ${color}`;
		}
		return '';
	}

	function slotLabel(option: TimeOptionResponse): string {
		if (mode === 'setup' || mode === 'edit') return '';
		if (mode === 'overview') {
			const yes = option.votes.filter((v) => v.vote === 'YES').length;
			const maybe = option.votes.filter((v) => v.vote === 'MAYBE').length;
			const no = option.votes.filter((v) => v.vote === 'NO').length;
			if (yes + maybe + no === 0) return '';
			return `${yes}/${maybe}/${no}`;
		}
		// Vote mode
		const myVote = getMyVote(option);
		if (myVote && myVote !== 'NONE') return myVote;
		return '';
	}

	// --- Vote popover ---
	function openVotePopover(e: MouseEvent, option: TimeOptionResponse) {
		if (!participantName) return;
		e.stopPropagation();
		hideTooltip();
		votePopoverOption = option;
		const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
		popoverX = rect.left + rect.width / 2;
		popoverY = rect.bottom + 4;
	}

	function closeVotePopover() {
		votePopoverOption = null;
	}

	async function submitVote(option: TimeOptionResponse, vote: string) {
		if (!participantName) return;
		error = '';
		closeVotePopover();
		try {
			await scheduling.castVotes(eventId, {
				participantName,
				votes: [{ timeOptionId: option.id, vote }],
			});
			onVoteCast?.();
		} catch {
			error = 'Failed to cast vote';
		}
	}

	// --- Setup mode: stamp/paint/delete ---
	async function handleStamp(dayDate: Date, rowIndex: number) {
		if (!adminToken) return;
		const startDt = cellDateTime(dayDate, rowIndex);
		const endDt = new Date(startDt.getTime() + durationMinutes * 60 * 1000);
		error = '';
		try {
			await scheduling.addTimeOptions(
				eventId,
				{
					options: [
						{
							startTime: startDt.toISOString(),
							endTime: endDt.toISOString(),
						},
					],
				},
				adminToken,
			);
			onOptionsChanged?.();
		} catch {
			error = 'Failed to add time option';
		}
	}

	async function handleDelete(option: TimeOptionResponse) {
		if (!adminToken) return;
		error = '';
		try {
			await scheduling.deleteTimeOption(eventId, option.id, adminToken);
			onOptionsChanged?.();
		} catch {
			error = 'Failed to delete time option';
		}
	}

	function handleCellMouseDown(dayIndex: number, rowIndex: number) {
		if (mode !== 'setup' && mode !== 'edit') return;
		const key = `${dayIndex}-${rowIndex}`;
		const existing = cellMap.get(key);
		if (existing) {
			void handleDelete(existing.option);
			return;
		}
		if (interactionMode === 'stamp') {
			void handleStamp(days[dayIndex], rowIndex);
			return;
		}
		isPainting = true;
		paintStart = { day: dayIndex, row: rowIndex };
		paintEnd = { day: dayIndex, row: rowIndex };
	}

	function handleCellMouseEnter(dayIndex: number, rowIndex: number) {
		if (!isPainting || !paintStart) return;
		paintEnd = { day: paintStart.day, row: rowIndex };
	}

	function handleMouseUp() {
		if (!isPainting || !paintStart || !paintEnd || !adminToken) {
			isPainting = false;
			paintStart = null;
			paintEnd = null;
			return;
		}
		const dayIndex = paintStart.day;
		const startRow = Math.min(paintStart.row, paintEnd.row);
		const endRow = Math.max(paintStart.row, paintEnd.row);
		const startDt = cellDateTime(days[dayIndex], startRow);
		const endDt = cellDateTime(days[dayIndex], endRow + 1);
		const paintedMinutes = (endRow - startRow + 1) * ROW_MINUTES;
		durationHours = paintedMinutes / 60;
		isPainting = false;
		paintStart = null;
		paintEnd = null;
		error = '';
		scheduling
			.addTimeOptions(
				eventId,
				{
					options: [{ startTime: startDt.toISOString(), endTime: endDt.toISOString() }],
				},
				adminToken,
			)
			.then(() => onOptionsChanged?.())
			.catch(() => {
				error = 'Failed to add time option';
			});
	}

	function isPaintCell(dayIndex: number, rowIndex: number): boolean {
		if (!isPainting || !paintStart || !paintEnd) return false;
		if (dayIndex !== paintStart.day) return false;
		const minRow = Math.min(paintStart.row, paintEnd.row);
		const maxRow = Math.max(paintStart.row, paintEnd.row);
		return rowIndex >= minRow && rowIndex <= maxRow;
	}

	function handleKeyDown(e: KeyboardEvent) {
		if ((mode === 'setup' || mode === 'edit') && e.key === 'Shift') {
			interactionMode = interactionMode === 'stamp' ? 'paint' : 'stamp';
		}
	}

	// --- Slot click handler ---
	function handleSlotClick(e: MouseEvent, option: TimeOptionResponse) {
		e.stopPropagation();
		if (mode === 'setup' || mode === 'edit') {
			void handleDelete(option);
		} else if (mode === 'vote') {
			openVotePopover(e, option);
		} else if (mode === 'overview' && adminToken && onDecide) {
			onDecide(option);
		}
	}

	// --- Tooltip ---
	function showTooltip(e: MouseEvent, option: TimeOptionResponse) {
		if (mode === 'setup' || mode === 'edit') return;
		tooltipOption = option;
		tooltipX = e.clientX;
		tooltipY = e.clientY;
	}

	function hideTooltip() {
		tooltipOption = null;
	}

	// Touch
	function handleTouchStart(dayIndex: number, rowIndex: number) {
		handleCellMouseDown(dayIndex, rowIndex);
	}

	function handleTouchEnd() {
		handleMouseUp();
	}
</script>

<svelte:window onkeydown={handleKeyDown} />

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="space-y-4" onmouseup={handleMouseUp} onmouseleave={handleMouseUp}>
	{#if mode === 'setup' || mode === 'edit'}
		<div class="flex flex-wrap items-center gap-4">
			<div class="flex items-center gap-2">
				<label class="label text-sm font-semibold" for="slot-duration">Duration</label>
				{#each durationPresets as preset (preset.value)}
					<button
						class="btn btn-sm {durationMinutes === preset.value
							? 'preset-filled-primary-500'
							: 'preset-tonal'}"
						onclick={() => {
							durationHours = preset.value / 60;
						}}
					>
						{preset.label}
					</button>
				{/each}
				<input
					id="slot-duration"
					class="input w-20 text-center"
					type="number"
					min="0.5"
					step="0.5"
					bind:value={durationHours}
				/>
				<span class="text-xs opacity-50">hours</span>
			</div>
			<div class="flex items-center gap-2">
				<span class="text-sm font-semibold">Mode</span>
				<button
					class="btn {interactionMode === 'stamp' ? 'preset-filled-primary-500' : 'preset-tonal'}"
					onclick={() => {
						interactionMode = 'stamp';
					}}
				>
					Stamp
				</button>
				<button
					class="btn {interactionMode === 'paint' ? 'preset-filled-primary-500' : 'preset-tonal'}"
					onclick={() => {
						interactionMode = 'paint';
					}}
				>
					Paint
				</button>
				<span class="text-xs opacity-50">(Shift to toggle)</span>
			</div>
		</div>
	{/if}

	<!-- Week navigation -->
	<div class="flex items-center justify-between">
		<button
			class="btn preset-tonal relative"
			onclick={() => {
				weekOffset -= 1;
			}}
		>
			{#if hasOptionsBefore}
				<span class="absolute -top-1 -left-1 w-2.5 h-2.5 rounded-full bg-primary-500"></span>
			{/if}
			&larr; Prev
		</button>
		<span class="font-semibold text-sm">{weekLabel}</span>
		<button
			class="btn preset-tonal relative"
			onclick={() => {
				weekOffset += 1;
			}}
		>
			{#if hasOptionsAfter}
				<span class="absolute -top-1 -right-1 w-2.5 h-2.5 rounded-full bg-primary-500"></span>
			{/if}
			Next &rarr;
		</button>
	</div>

	{#if error}
		<p class="text-error-500 text-sm">{error}</p>
	{/if}

	{#if mode === 'overview'}
		<div class="flex items-center gap-4 text-xs">
			<span class="flex items-center gap-1">
				<span class="inline-block w-3 h-3 rounded" style="background-color: hsl(120, 70%, 45%)"
				></span> Most Yes
			</span>
			<span class="flex items-center gap-1">
				<span class="inline-block w-3 h-3 rounded" style="background-color: hsl(60, 70%, 45%)"
				></span> Mixed
			</span>
			<span class="flex items-center gap-1">
				<span class="inline-block w-3 h-3 rounded" style="background-color: hsl(0, 70%, 45%)"
				></span> Most No
			</span>
			<span class="flex items-center gap-1">
				<span class="inline-block w-3 h-3 rounded" style="background-color: hsl(0, 0%, 60%)"></span> No
				votes
			</span>
			<span class="opacity-50 ml-2">Labels: yes/maybe/no</span>
		</div>
	{/if}

	{#if mode === 'vote'}
		<div class="flex items-center gap-4 text-xs">
			<span class="flex items-center gap-1">
				<span class="inline-block w-3 h-3 rounded" style="background-color: var(--vote-yes)"></span> Yes
			</span>
			<span class="flex items-center gap-1">
				<span class="inline-block w-3 h-3 rounded" style="background-color: var(--vote-maybe)"
				></span> Maybe
			</span>
			<span class="flex items-center gap-1">
				<span class="inline-block w-3 h-3 rounded" style="background-color: var(--vote-no)"></span> No
			</span>
			<span class="flex items-center gap-1">
				<span
					class="inline-block w-3 h-3 rounded"
					style="background-color: var(--vote-none); opacity: 0.4"
				></span> Not voted
			</span>
		</div>
	{/if}

	<!-- Grid -->
	<div class="overflow-x-auto">
		<table class="table-auto border-collapse select-none w-full" aria-label="Time scheduling grid">
			<thead>
				<tr>
					<th class="p-1 text-xs w-14" scope="col"></th>
					{#each days as day, dayIndex (dayIndex)}
						<th class="p-1 text-xs text-center" scope="col">{dayHeader(day)}</th>
					{/each}
				</tr>
			</thead>
			<tbody>
				{#each rowLabels as label, rowIndex (rowIndex)}
					<tr>
						<td class="p-0 text-xs text-right pr-1 opacity-75 align-top leading-none">{label}</td>
						{#each [0, 1, 2, 3, 4, 5, 6] as dayIndex (dayIndex)}
							{@const key = `${dayIndex}-${rowIndex}`}
							{@const cell = cellMap.get(key)}
							{@const painting = isPaintCell(dayIndex, rowIndex)}
							<td
								class="relative h-6 min-w-10 border border-surface-300-700/50 p-0
									{painting ? 'bg-primary-500/30' : ''}
									{!cell && (mode === 'setup' || mode === 'edit') ? 'cursor-pointer hover:bg-surface-200-800' : ''}
									{cell && mode !== 'setup' && mode !== 'edit' ? 'cursor-pointer' : ''}"
								onmousedown={() => handleCellMouseDown(dayIndex, rowIndex)}
								onmouseenter={() => handleCellMouseEnter(dayIndex, rowIndex)}
								ontouchstart={() => handleTouchStart(dayIndex, rowIndex)}
								ontouchend={handleTouchEnd}
							>
								{#if cell?.isStart}
									<button
										type="button"
										class="absolute inset-x-0 top-0 z-10 rounded-sm text-xs flex items-center justify-center font-medium border-0
											{slotColor()}
											{mode === 'overview' && adminToken
											? 'cursor-pointer hover:opacity-80 hover:ring-2 hover:ring-white/50'
											: 'cursor-pointer hover:opacity-80'}"
										style="height: {cell.spanRows * 100}%; {slotInlineStyle(cell.option)}"
										aria-label="{new Date(cell.option.startTime).toLocaleTimeString(undefined, {
											hour: '2-digit',
											minute: '2-digit',
										})} - {new Date(cell.option.endTime).toLocaleTimeString(undefined, {
											hour: '2-digit',
											minute: '2-digit',
										})}{getMyVote(cell.option) ? `, voted ${getMyVote(cell.option)}` : ''}"
										onclick={(e) => handleSlotClick(e, cell.option)}
										onmouseenter={(e) => showTooltip(e, cell.option)}
										onmouseleave={hideTooltip}
									>
										{#if slotLabel(cell.option)}
											<span class="text-[10px] text-white drop-shadow"
												>{slotLabel(cell.option)}</span
											>
										{/if}
									</button>
								{/if}
							</td>
						{/each}
					</tr>
				{/each}
			</tbody>
		</table>
	</div>

	{#if mode === 'setup' || mode === 'edit'}
		<p class="text-xs opacity-50">
			Click empty cells to add time slots. Click existing slots to delete them.
			{interactionMode === 'paint'
				? 'Drag to paint a range. The painted length becomes the new stamp duration.'
				: 'Each click stamps a slot of the selected duration.'}
		</p>
	{:else if mode === 'vote'}
		<p class="text-xs opacity-50">Click on a slot to vote: Yes, Maybe, or No.</p>
	{:else if mode === 'overview' && adminToken}
		<div class="card preset-tonal-warning-500 p-3 text-sm flex items-center gap-2">
			<span class="font-semibold">Admin:</span> Click on any time slot to select it as the final date
			for this event.
		</div>
	{/if}

	<!-- Vote popover (vote mode only) -->
	{#if votePopoverOption && mode === 'vote'}
		{@const myVote = getMyVote(votePopoverOption)}
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<div
			class="fixed inset-0 z-40"
			onclick={closeVotePopover}
			onkeydown={(e) => {
				if (e.key === 'Escape') closeVotePopover();
			}}
		></div>
		<div
			class="fixed z-50 card bg-surface-100-900 p-3 shadow-xl space-y-2"
			style="left: {popoverX}px; top: {popoverY}px; transform: translateX(-50%)"
		>
			<p class="text-xs font-semibold opacity-75">Your vote</p>
			<div class="flex gap-1">
				<button
					class="btn btn-sm text-white"
					style="background-color: var(--vote-yes); opacity: {myVote === 'YES' ? 1 : 0.6}"
					onclick={() => submitVote(votePopoverOption!, 'YES')}
				>
					Yes
				</button>
				<button
					class="btn btn-sm text-white"
					style="background-color: var(--vote-maybe); opacity: {myVote === 'MAYBE' ? 1 : 0.6}"
					onclick={() => submitVote(votePopoverOption!, 'MAYBE')}
				>
					Maybe
				</button>
				<button
					class="btn btn-sm text-white"
					style="background-color: var(--vote-no); opacity: {myVote === 'NO' ? 1 : 0.6}"
					onclick={() => submitVote(votePopoverOption!, 'NO')}
				>
					No
				</button>
			</div>
			{#if myVote}
				<button
					class="btn btn-sm preset-tonal w-full text-xs"
					onclick={() => submitVote(votePopoverOption!, 'NONE')}
				>
					Clear vote
				</button>
			{/if}
		</div>
	{/if}

	<!-- Tooltip (vote + overview modes) -->
	{#if tooltipOption && mode !== 'setup' && !votePopoverOption}
		<div
			class="fixed z-50 card bg-surface-100-900 p-2 shadow-xl text-xs space-y-1 pointer-events-none"
			style="left: {tooltipX + 12}px; top: {tooltipY + 12}px"
		>
			{#each tooltipOption.votes as v (v.id)}
				<div>
					<span class="font-semibold">{v.participantName}:</span>
					<span
						style={v.vote === 'YES'
							? 'color: var(--vote-yes)'
							: v.vote === 'MAYBE'
								? 'color: var(--vote-maybe)'
								: v.vote === 'NO'
									? 'color: var(--vote-no)'
									: ''}>{v.vote}</span
					>
				</div>
			{:else}
				<div class="opacity-50">No votes yet</div>
			{/each}
		</div>
	{/if}
</div>

<style>
	:root {
		--vote-yes: #22c55e;
		--vote-maybe: #eab308;
		--vote-no: #ef4444;
		--vote-none: #9ca3af;
	}
</style>
