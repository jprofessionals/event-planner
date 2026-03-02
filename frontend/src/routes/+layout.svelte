<script lang="ts">
	import '../app.css';
	import type { Snippet } from 'svelte';
	import { resolve } from '$app/paths';
	import { auth } from '$lib/stores/auth';
	import { CalendarIcon, LogInIcon, LayoutDashboardIcon, PlusIcon } from '@lucide/svelte';

	let { children }: { children: Snippet } = $props();
	let mobileMenuOpen = $state(false);
</script>

<div class="min-h-screen flex flex-col">
	<nav class="bg-surface-100-900 border-b border-surface-300-700 px-4 py-3">
		<div class="container mx-auto flex items-center justify-between">
			<a href={resolve('/')} class="flex items-center gap-2 font-bold text-lg">
				<CalendarIcon class="w-5 h-5 text-primary-500" />
				Event Planner
			</a>

			<div class="hidden sm:flex items-center gap-2">
				{#if $auth.token}
					<a href={resolve('/dashboard')} class="btn btn-sm preset-tonal">
						<LayoutDashboardIcon class="w-4 h-4" />
						Dashboard
					</a>
					<a href="{resolve('/')}?create" class="btn btn-sm preset-filled-primary-500">
						<PlusIcon class="w-4 h-4" />
						New Event
					</a>
				{:else}
					<a href={resolve('/login')} class="btn btn-sm preset-tonal">
						<LogInIcon class="w-4 h-4" />
						Log In
					</a>
					<a href={resolve('/register')} class="btn btn-sm preset-filled-primary-500">Sign Up</a>
				{/if}
			</div>

			<button
				class="sm:hidden btn btn-sm preset-tonal"
				onclick={() => {
					mobileMenuOpen = !mobileMenuOpen;
				}}
				aria-label="Toggle menu"
			>
				<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
					{#if mobileMenuOpen}
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M6 18L18 6M6 6l12 12"
						/>
					{:else}
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M4 6h16M4 12h16M4 18h16"
						/>
					{/if}
				</svg>
			</button>
		</div>

		{#if mobileMenuOpen}
			<div class="sm:hidden mt-3 pt-3 border-t border-surface-300-700 flex flex-col gap-2">
				{#if $auth.token}
					<a
						href={resolve('/dashboard')}
						class="btn btn-sm preset-tonal w-full"
						onclick={() => {
							mobileMenuOpen = false;
						}}>Dashboard</a
					>
					<a
						href="{resolve('/')}?create"
						class="btn btn-sm preset-filled-primary-500 w-full"
						onclick={() => {
							mobileMenuOpen = false;
						}}>New Event</a
					>
				{:else}
					<a
						href={resolve('/login')}
						class="btn btn-sm preset-tonal w-full"
						onclick={() => {
							mobileMenuOpen = false;
						}}>Log In</a
					>
					<a
						href={resolve('/register')}
						class="btn btn-sm preset-filled-primary-500 w-full"
						onclick={() => {
							mobileMenuOpen = false;
						}}>Sign Up</a
					>
				{/if}
			</div>
		{/if}
	</nav>

	<main class="flex-1">
		{@render children()}
	</main>
</div>
