<script lang="ts">
	let {
		onSubmit,
		error = '',
		loading = false,
		prefillName = '',
	}: {
		onSubmit: (passphrase: string, displayName: string) => void;
		error?: string;
		loading?: boolean;
		prefillName?: string;
	} = $props();

	let passphrase = $state('');
	let displayName = $state(prefillName);

	const nameResolved = $derived(prefillName || displayName.trim());

	function handleSubmit(e: Event) {
		e.preventDefault();
		if (!passphrase.trim() || !nameResolved) return;
		onSubmit(passphrase.trim(), nameResolved);
	}
</script>

<form class="space-y-4" onsubmit={handleSubmit}>
	{#if !prefillName}
		<div>
			<label class="label text-sm font-semibold" for="display-name">Your Name</label>
			<input
				id="display-name"
				class="input"
				type="text"
				placeholder="Enter your display name"
				bind:value={displayName}
				required
			/>
		</div>
	{/if}
	<div>
		<label class="label text-sm font-semibold" for="passphrase">Passphrase</label>
		<input
			id="passphrase"
			class="input"
			type="password"
			placeholder="Enter the event passphrase"
			bind:value={passphrase}
			required
		/>
	</div>

	{#if error}
		<p class="text-error-500">{error}</p>
	{/if}

	<button
		class="btn preset-filled-primary-500 w-full"
		type="submit"
		disabled={loading || !passphrase.trim() || !nameResolved}
	>
		{loading ? 'Joining...' : 'Join Event'}
	</button>
</form>
