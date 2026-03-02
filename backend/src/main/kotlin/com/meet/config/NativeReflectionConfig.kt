package com.meet.config

import com.meet.auth.AuthResponse
import com.meet.auth.LoginRequest
import com.meet.auth.RegisterRequest
import com.meet.auth.UserResponse
import com.meet.checklist.AddChecklistItemRequest
import com.meet.checklist.ChecklistItemResponse
import com.meet.checklist.UpdateChecklistItemRequest
import com.meet.comment.AddCommentRequest
import com.meet.comment.CommentResponse
import com.meet.event.CreateEventRequest
import com.meet.event.DecideTimeRequest
import com.meet.event.EventResponse
import com.meet.event.JoinEventRequest
import com.meet.event.JoinEventResponse
import com.meet.event.UpdateEventRequest
import com.meet.poll.CreatePollRequest
import com.meet.poll.PollOptionResponse
import com.meet.poll.PollResponse
import com.meet.poll.VotePollRequest
import com.meet.scheduling.AddTimeOptionsRequest
import com.meet.scheduling.CastVotesRequest
import com.meet.scheduling.TimeOptionInput
import com.meet.scheduling.TimeOptionResponse
import com.meet.scheduling.VoteInput
import com.meet.scheduling.VoteResponse
import com.meet.shoppinglist.CreateShoppingListRequest
import com.meet.shoppinglist.ExternalCreateRequest
import com.meet.shoppinglist.ExternalCreateResponse
import com.meet.shoppinglist.ShoppingListResponse
import com.meet.sse.SseEvent
import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection(
    targets = [
        // Auth
        RegisterRequest::class,
        LoginRequest::class,
        AuthResponse::class,
        UserResponse::class,
        // Event
        CreateEventRequest::class,
        UpdateEventRequest::class,
        EventResponse::class,
        JoinEventRequest::class,
        JoinEventResponse::class,
        DecideTimeRequest::class,
        // Scheduling
        AddTimeOptionsRequest::class,
        TimeOptionInput::class,
        TimeOptionResponse::class,
        CastVotesRequest::class,
        VoteInput::class,
        VoteResponse::class,
        // Poll
        CreatePollRequest::class,
        PollResponse::class,
        PollOptionResponse::class,
        VotePollRequest::class,
        // Checklist
        AddChecklistItemRequest::class,
        UpdateChecklistItemRequest::class,
        ChecklistItemResponse::class,
        // Comment
        AddCommentRequest::class,
        CommentResponse::class,
        // Shopping list
        CreateShoppingListRequest::class,
        ShoppingListResponse::class,
        ExternalCreateRequest::class,
        ExternalCreateResponse::class,
        // SSE
        SseEvent::class,
    ],
)
class NativeReflectionConfig
