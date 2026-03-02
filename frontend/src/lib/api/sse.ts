export function subscribeToEvent(
	eventId: string,
	onMessage: (type: string, data: Record<string, unknown>) => void,
): () => void {
	const source = new EventSource(`/api/events/${eventId}/stream`);

	source.onmessage = (e) => {
		try {
			const data = JSON.parse(e.data as string) as Record<string, unknown>;
			onMessage((data.type as string) || 'message', data);
		} catch {
			// Ignore non-JSON messages
		}
	};

	source.onerror = () => {
		// EventSource will automatically reconnect
	};

	return () => source.close();
}
