<script lang="ts">
	import { comments, type CommentResponse } from '$lib/api/client';

	let {
		eventId,
		participantName,
	}: {
		eventId: string;
		participantName: string;
	} = $props();

	let commentList = $state<CommentResponse[]>([]);
	let loading = $state(true);
	let error = $state('');
	let newContent = $state('');
	let posting = $state(false);

	async function loadComments() {
		loading = true;
		try {
			commentList = await comments.list(eventId);
		} catch {
			error = 'Failed to load comments';
		} finally {
			loading = false;
		}
	}

	async function addComment() {
		if (!newContent.trim()) return;
		posting = true;
		error = '';
		try {
			const comment = await comments.add(eventId, {
				authorName: participantName,
				content: newContent.trim(),
			});
			commentList = [...commentList, comment];
			newContent = '';
		} catch {
			error = 'Failed to post comment';
		} finally {
			posting = false;
		}
	}

	function formatTimestamp(iso: string): string {
		return new Date(iso).toLocaleString(undefined, {
			month: 'short',
			day: 'numeric',
			hour: '2-digit',
			minute: '2-digit',
		});
	}

	export function refresh() {
		void loadComments();
	}

	$effect(() => {
		void loadComments();
	});
</script>

<div class="space-y-4">
	<h2 class="h3">Comments</h2>

	{#if loading}
		<p class="opacity-75">Loading comments...</p>
	{:else}
		<div class="space-y-3">
			{#each commentList as comment (comment.id)}
				<div class="card bg-surface-100-900 p-3 space-y-1">
					<div class="flex justify-between items-center">
						<span class="font-semibold text-sm">{comment.authorName}</span>
						<span class="text-xs opacity-50">{formatTimestamp(comment.createdAt)}</span>
					</div>
					<p>{comment.content}</p>
				</div>
			{/each}
		</div>

		{#if commentList.length === 0}
			<p class="opacity-75 text-sm">No comments yet. Start the conversation.</p>
		{/if}

		{#if error}
			<p class="text-error-500">{error}</p>
		{/if}

		<form
			class="flex gap-2"
			onsubmit={(e) => {
				e.preventDefault();
				void addComment();
			}}
		>
			<input
				class="input flex-1"
				type="text"
				placeholder="Write a comment..."
				bind:value={newContent}
			/>
			<button
				class="btn preset-filled-primary-500"
				type="submit"
				disabled={posting || !newContent.trim()}
			>
				{posting ? 'Posting...' : 'Post'}
			</button>
		</form>
	{/if}
</div>
