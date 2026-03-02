<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { auth as authApi, ApiError } from '$lib/api/client';
	import { auth } from '$lib/stores/auth';

	let email = $state('');
	let password = $state('');
	let displayName = $state('');
	let loading = $state(false);
	let error = $state('');

	async function handleRegister(e: Event) {
		e.preventDefault();
		if (!email.trim() || !password || !displayName.trim()) return;

		loading = true;
		error = '';
		try {
			const result = await authApi.register({
				email: email.trim(),
				password,
				displayName: displayName.trim(),
			});
			auth.login(result.token, result.user);
			void goto(resolve('/dashboard'));
		} catch (err) {
			if (err instanceof ApiError) {
				if (err.status === 409) {
					error = 'An account with this email already exists.';
				} else {
					error = `Registration failed: ${err.body}`;
				}
			} else {
				error = 'Registration failed. Please try again.';
			}
		} finally {
			loading = false;
		}
	}
</script>

<div class="container mx-auto max-w-md p-8 space-y-6">
	<header class="text-center space-y-2">
		<h1 class="h2">Register</h1>
		<p class="opacity-75">Create an account to manage your events.</p>
	</header>

	<div class="card bg-surface-100-900 p-6 space-y-6 shadow-xl">
		<form class="space-y-4" onsubmit={handleRegister}>
			<div>
				<label class="label text-sm font-semibold" for="display-name">Display Name</label>
				<input
					id="display-name"
					class="input"
					type="text"
					placeholder="Your name"
					bind:value={displayName}
					required
				/>
			</div>

			<div>
				<label class="label text-sm font-semibold" for="email">Email</label>
				<input
					id="email"
					class="input"
					type="email"
					placeholder="you@example.com"
					bind:value={email}
					required
				/>
			</div>

			<div>
				<label class="label text-sm font-semibold" for="password">Password</label>
				<input
					id="password"
					class="input"
					type="password"
					placeholder="Choose a password"
					bind:value={password}
					required
				/>
			</div>

			{#if error}
				<p class="text-error-500">{error}</p>
			{/if}

			<button
				class="btn preset-filled-primary-500 w-full"
				type="submit"
				disabled={loading || !email.trim() || !password || !displayName.trim()}
			>
				{loading ? 'Creating account...' : 'Register'}
			</button>
		</form>
	</div>

	<div class="text-center space-y-2">
		<p class="text-sm opacity-50">Already have an account?</p>
		<a href={resolve('/login')} class="btn preset-tonal">Log In</a>
	</div>
</div>
