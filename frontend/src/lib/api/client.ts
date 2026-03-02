const BASE = '';

export class ApiError extends Error {
	constructor(
		public status: number,
		public body: string,
	) {
		super(`API Error ${status}`);
	}
}

async function request<T>(
	method: string,
	path: string,
	body?: unknown,
	headers?: Record<string, string>,
): Promise<T> {
	const reqHeaders: Record<string, string> = { ...headers };
	if (body) reqHeaders['Content-Type'] = 'application/json';
	const opts: RequestInit = {
		method,
		headers: reqHeaders,
		cache: 'no-store',
	};
	if (body) opts.body = JSON.stringify(body);
	const res = await fetch(`${BASE}${path}`, opts);
	if (!res.ok) throw new ApiError(res.status, await res.text());
	if (res.status === 204) return undefined as T;
	return res.json() as Promise<T>;
}

// --- Event types ---

export interface CreateEventRequest {
	title: string;
	description?: string;
	secretVotes?: boolean;
	participantsCanPoll?: boolean;
	participantsCanChecklist?: boolean;
	participantsCanShoppingList?: boolean;
	passphrase: string;
}

export interface UpdateEventRequest {
	title?: string;
	description?: string;
	passphrase?: string;
	participantsCanPoll?: boolean;
	participantsCanChecklist?: boolean;
	participantsCanShoppingList?: boolean;
}

export interface EventResponse {
	id: string;
	title: string;
	description: string | null;
	secretVotes: boolean;
	participantsCanPoll: boolean;
	participantsCanChecklist: boolean;
	participantsCanShoppingList: boolean;
	stage: string;
	adminToken?: string | null;
	passphrase?: string | null;
	decidedTimeStart: string | null;
	decidedTimeEnd: string | null;
	createdAt: string;
}

export interface JoinEventRequest {
	passphrase: string;
	displayName: string;
}

export interface JoinEventResponse {
	participantId: string;
	displayName: string;
	eventId: string;
}

export interface DecideTimeRequest {
	startTime: string;
	endTime: string;
}

// --- Scheduling types ---

export interface TimeOptionInput {
	startTime: string;
	endTime: string;
}

export interface AddTimeOptionsRequest {
	options: TimeOptionInput[];
}

export interface VoteResponse {
	id: string;
	participantName: string;
	vote: string;
}

export interface TimeOptionResponse {
	id: string;
	startTime: string;
	endTime: string;
	votes: VoteResponse[];
}

export interface VoteInput {
	timeOptionId: string;
	vote: string;
}

export interface CastVotesRequest {
	participantName: string;
	votes: VoteInput[];
}

// --- Poll types ---

export interface CreatePollRequest {
	question: string;
	options: string[];
	allowMultiple?: boolean;
}

export interface PollOptionResponse {
	id: string;
	text: string;
	voteCount: number;
}

export interface PollResponse {
	id: string;
	question: string;
	allowMultiple: boolean;
	options: PollOptionResponse[];
	createdAt: string;
}

export interface VotePollRequest {
	participantName: string;
	optionIds: string[];
}

// --- Checklist types ---

export interface AddChecklistItemRequest {
	text: string;
	assignedTo?: string;
}

export interface UpdateChecklistItemRequest {
	completed?: boolean;
	text?: string;
	assignedTo?: string;
}

export interface ChecklistItemResponse {
	id: string;
	text: string;
	completed: boolean;
	assignedTo: string | null;
	createdAt: string;
}

// --- Comment types ---

export interface AddCommentRequest {
	authorName: string;
	content: string;
}

export interface CommentResponse {
	id: string;
	authorName: string;
	content: string;
	createdAt: string;
}

// --- Shopping List types ---

export interface CreateShoppingListRequest {
	title: string;
	email?: string;
}

export interface ShoppingListResponse {
	id: string;
	eventId: string;
	title: string;
	shareToken: string;
	widgetUrl: string;
	createdByParticipant: string;
	createdAt: string;
}

// --- Auth types ---

export interface RegisterRequest {
	email: string;
	password: string;
	displayName: string;
}

export interface LoginRequest {
	email: string;
	password: string;
}

export interface UserResponse {
	id: string;
	email: string;
	displayName: string;
}

export interface AuthResponse {
	token: string;
	user: UserResponse;
}

// --- API functions ---

export const events = {
	create: (data: CreateEventRequest, authToken?: string) =>
		request<EventResponse>(
			'POST',
			'/api/events',
			data,
			authToken ? { Authorization: `Bearer ${authToken}` } : undefined,
		),

	get: (id: string, adminToken?: string) =>
		request<EventResponse>(
			'GET',
			`/api/events/${id}`,
			undefined,
			adminToken ? { 'X-Admin-Token': adminToken } : undefined,
		),

	update: (id: string, data: UpdateEventRequest, adminToken: string) =>
		request<EventResponse>('PATCH', `/api/events/${id}`, data, { 'X-Admin-Token': adminToken }),

	join: (id: string, data: JoinEventRequest, authToken?: string) =>
		request<JoinEventResponse>(
			'POST',
			`/api/events/${id}/join`,
			data,
			authToken ? { Authorization: `Bearer ${authToken}` } : undefined,
		),

	decide: (id: string, data: DecideTimeRequest, adminToken: string) =>
		request<EventResponse>('POST', `/api/events/${id}/decide`, data, {
			'X-Admin-Token': adminToken,
		}),
};

export const scheduling = {
	addTimeOptions: (eventId: string, data: AddTimeOptionsRequest, adminToken: string) =>
		request<TimeOptionResponse[]>('POST', `/api/events/${eventId}/time-options`, data, {
			'X-Admin-Token': adminToken,
		}),

	getTimeOptions: (eventId: string, opts?: { participantName?: string; adminToken?: string }) => {
		const headers: Record<string, string> = {};
		if (opts?.participantName) headers['X-Participant-Name'] = opts.participantName;
		if (opts?.adminToken) headers['X-Admin-Token'] = opts.adminToken;
		return request<TimeOptionResponse[]>(
			'GET',
			`/api/events/${eventId}/time-options`,
			undefined,
			Object.keys(headers).length > 0 ? headers : undefined,
		);
	},

	deleteTimeOption: (eventId: string, optionId: string, adminToken: string) =>
		request<void>('DELETE', `/api/events/${eventId}/time-options/${optionId}`, undefined, {
			'X-Admin-Token': adminToken,
		}),

	castVotes: (eventId: string, data: CastVotesRequest) =>
		request<void>('POST', `/api/events/${eventId}/votes`, data),
};

export const polls = {
	create: (eventId: string, data: CreatePollRequest, adminToken?: string) =>
		request<PollResponse>(
			'POST',
			`/api/events/${eventId}/polls`,
			data,
			adminToken ? { 'X-Admin-Token': adminToken } : undefined,
		),

	list: (eventId: string) => request<PollResponse[]>('GET', `/api/events/${eventId}/polls`),

	vote: (eventId: string, pollId: string, data: VotePollRequest) =>
		request<void>('POST', `/api/events/${eventId}/polls/${pollId}/vote`, data),
};

export const checklist = {
	add: (eventId: string, data: AddChecklistItemRequest, adminToken?: string) =>
		request<ChecklistItemResponse>(
			'POST',
			`/api/events/${eventId}/checklist`,
			data,
			adminToken ? { 'X-Admin-Token': adminToken } : undefined,
		),

	list: (eventId: string) =>
		request<ChecklistItemResponse[]>('GET', `/api/events/${eventId}/checklist`),

	update: (eventId: string, itemId: string, data: UpdateChecklistItemRequest) =>
		request<ChecklistItemResponse>('PATCH', `/api/events/${eventId}/checklist/${itemId}`, data),
};

export const comments = {
	list: (eventId: string) => request<CommentResponse[]>('GET', `/api/events/${eventId}/comments`),

	add: (eventId: string, data: AddCommentRequest) =>
		request<CommentResponse>('POST', `/api/events/${eventId}/comments`, data),
};

export const shoppingLists = {
	create: (eventId: string, data: CreateShoppingListRequest, adminToken?: string) =>
		request<ShoppingListResponse>(
			'POST',
			`/api/events/${eventId}/shopping-lists`,
			data,
			adminToken ? { 'X-Admin-Token': adminToken } : undefined,
		),

	list: (eventId: string) =>
		request<ShoppingListResponse[]>('GET', `/api/events/${eventId}/shopping-lists`),

	remove: (eventId: string, listId: string, adminToken: string) =>
		request<void>('DELETE', `/api/events/${eventId}/shopping-lists/${listId}`, undefined, {
			'X-Admin-Token': adminToken,
		}),
};

export const auth = {
	register: (data: RegisterRequest) => request<AuthResponse>('POST', '/api/auth/register', data),

	login: (data: LoginRequest) => request<AuthResponse>('POST', '/api/auth/login', data),

	me: (token: string) =>
		request<UserResponse>('GET', '/api/users/me', undefined, { Authorization: `Bearer ${token}` }),

	myEvents: (token: string) =>
		request<EventResponse[]>('GET', '/api/users/me/events', undefined, {
			Authorization: `Bearer ${token}`,
		}),
};
