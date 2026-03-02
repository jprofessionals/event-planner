<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { auth as authApi, ApiError } from '$lib/api/client';
	import { auth } from '$lib/stores/auth';

	let email = $state('');
	let password = $state('');
	let loading = $state(false);
	let error = $state('');

	async function handleLogin(e: Event) {
		e.preventDefault();
		if (!email.trim() || !password) return;

		loading = true;
		error = '';
		try {
			const result = await authApi.login({ email: email.trim(), password });
			auth.login(result.token, result.user);
			void goto(resolve('/dashboard'));
		} catch (err) {
			if (err instanceof ApiError) {
				if (err.status === 401) {
					error = 'Invalid email or password.';
				} else {
					error = `Login failed: ${err.body}`;
				}
			} else {
				error = 'Login failed. Please try again.';
			}
		} finally {
			loading = false;
		}
	}
</script>

<div class="container mx-auto max-w-md p-8 space-y-6">
	<header class="text-center space-y-2">
		<h1 class="h2">Log In</h1>
		<p class="opacity-75">Sign in to manage your events.</p>
	</header>

	<div class="card bg-surface-100-900 p-6 space-y-6 shadow-xl">
		<form class="space-y-4" onsubmit={handleLogin}>
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
					placeholder="Your password"
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
				disabled={loading || !email.trim() || !password}
			>
				{loading ? 'Logging in...' : 'Log In'}
			</button>
		</form>
	</div>

	<div class="text-center space-y-2">
		<p class="text-sm opacity-50">Don't have an account?</p>
		<a href={resolve('/register')} class="btn preset-tonal">Register</a>
	</div>
</div>
