package com.meet.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.meet.event.EventResponse
import com.meet.event.EventStage
import com.meet.generated.jooq.tables.EventParticipants.Companion.EVENT_PARTICIPANTS
import com.meet.generated.jooq.tables.Events.Companion.EVENTS
import com.meet.generated.jooq.tables.Users.Companion.USERS
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class AuthService(
    private val dsl: DSLContext,
    private val jwtService: JwtService,
) {
    fun register(request: RegisterRequest): AuthResponse {
        val existing =
            dsl
                .selectFrom(USERS)
                .where(USERS.EMAIL.eq(request.email))
                .fetchOne()

        if (existing != null) {
            throw WebApplicationException("Email already registered", Response.Status.CONFLICT)
        }

        val id = UUID.randomUUID()
        val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())

        dsl
            .insertInto(USERS)
            .set(USERS.ID, id)
            .set(USERS.EMAIL, request.email)
            .set(USERS.PASSWORD_HASH, passwordHash)
            .set(USERS.DISPLAY_NAME, request.displayName)
            .set(USERS.CREATED_AT, OffsetDateTime.now())
            .execute()

        val token = jwtService.generateToken(id, request.email)

        return AuthResponse(
            token = token,
            user =
                UserResponse(
                    id = id,
                    email = request.email,
                    displayName = request.displayName,
                ),
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user =
            dsl
                .selectFrom(USERS)
                .where(USERS.EMAIL.eq(request.email))
                .fetchOne() ?: throw WebApplicationException("Invalid credentials", Response.Status.UNAUTHORIZED)

        val result = BCrypt.verifyer().verify(request.password.toCharArray(), user.passwordHash)
        if (!result.verified) {
            throw WebApplicationException("Invalid credentials", Response.Status.UNAUTHORIZED)
        }

        val token = jwtService.generateToken(user.id, user.email)

        return AuthResponse(
            token = token,
            user =
                UserResponse(
                    id = user.id,
                    email = user.email,
                    displayName = user.displayName,
                ),
        )
    }

    fun getCurrentUser(userId: UUID): UserResponse {
        val user =
            dsl
                .selectFrom(USERS)
                .where(USERS.ID.eq(userId))
                .fetchOne() ?: throw WebApplicationException("User not found", Response.Status.NOT_FOUND)

        return UserResponse(
            id = user.id,
            email = user.email,
            displayName = user.displayName,
        )
    }

    fun getUserEvents(userId: UUID): List<EventResponse> {
        val ownedEvents =
            dsl
                .selectFrom(EVENTS)
                .where(EVENTS.OWNER_ID.eq(userId))
                .fetch()

        val participantEventIds =
            dsl
                .select(EVENT_PARTICIPANTS.EVENT_ID)
                .from(EVENT_PARTICIPANTS)
                .where(EVENT_PARTICIPANTS.USER_ID.eq(userId))
                .fetch()
                .map { it.value1()!! }

        val participantEvents =
            if (participantEventIds.isNotEmpty()) {
                dsl
                    .selectFrom(EVENTS)
                    .where(EVENTS.ID.`in`(participantEventIds))
                    .and(EVENTS.OWNER_ID.isNull.or(EVENTS.OWNER_ID.ne(userId)))
                    .fetch()
            } else {
                emptyList()
            }

        val ownedEventIds = ownedEvents.map { it.id }.toSet()

        return (ownedEvents + participantEvents).map { record ->
            EventResponse(
                id = record.id,
                title = record.title,
                description = record.description,
                secretVotes = record.secretVotes ?: false,
                participantsCanPoll = record.participantsCanPoll ?: true,
                participantsCanChecklist = record.participantsCanChecklist ?: true,
                participantsCanShoppingList = record.participantsCanShoppingList ?: false,
                stage = EventStage.valueOf(record.stage ?: "SCHEDULING"),
                adminToken = if (record.id in ownedEventIds) record.adminToken else null,
                decidedTimeStart = record.decidedTimeStart,
                decidedTimeEnd = record.decidedTimeEnd,
                createdAt = record.createdAt ?: OffsetDateTime.now(),
            )
        }
    }
}
