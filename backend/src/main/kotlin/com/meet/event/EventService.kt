package com.meet.event

import at.favre.lib.crypto.bcrypt.BCrypt
import com.meet.generated.jooq.tables.EventParticipants.Companion.EVENT_PARTICIPANTS
import com.meet.generated.jooq.tables.Events.Companion.EVENTS
import com.meet.generated.jooq.tables.Users.Companion.USERS
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class EventService(
    private val dsl: DSLContext,
) {
    fun createEvent(
        request: CreateEventRequest,
        ownerId: UUID? = null,
    ): EventResponse {
        val id = UUID.randomUUID()
        val adminToken = UUID.randomUUID()
        val now = OffsetDateTime.now()

        // Verify owner exists before setting foreign key
        val verifiedOwnerId =
            if (ownerId != null) {
                val exists = dsl.selectFrom(USERS).where(USERS.ID.eq(ownerId)).fetchOne()
                if (exists != null) ownerId else null
            } else {
                null
            }

        dsl
            .insertInto(EVENTS)
            .set(EVENTS.ID, id)
            .set(EVENTS.TITLE, request.title)
            .set(EVENTS.DESCRIPTION, request.description)
            .set(EVENTS.SECRET_VOTES, request.secretVotes)
            .set(EVENTS.PARTICIPANTS_CAN_POLL, request.participantsCanPoll)
            .set(EVENTS.PARTICIPANTS_CAN_CHECKLIST, request.participantsCanChecklist)
            .set(EVENTS.PARTICIPANTS_CAN_SHOPPING_LIST, request.participantsCanShoppingList)
            .set(EVENTS.STAGE, EventStage.SCHEDULING.name)
            .set(EVENTS.ADMIN_TOKEN, adminToken)
            .set(EVENTS.PASSPHRASE, BCrypt.withDefaults().hashToString(12, request.passphrase.toCharArray()))
            .set(EVENTS.OWNER_ID, verifiedOwnerId)
            .set(EVENTS.CREATED_AT, now)
            .execute()

        return EventResponse(
            id = id,
            title = request.title,
            description = request.description,
            secretVotes = request.secretVotes,
            participantsCanPoll = request.participantsCanPoll,
            participantsCanChecklist = request.participantsCanChecklist,
            participantsCanShoppingList = request.participantsCanShoppingList,
            stage = EventStage.SCHEDULING,
            adminToken = adminToken,
            passphrase = null,
            createdAt = now,
        )
    }

    fun getEvent(eventId: UUID): EventResponse {
        val record =
            dsl
                .selectFrom(EVENTS)
                .where(EVENTS.ID.eq(eventId))
                .fetchOne() ?: throw WebApplicationException("Event not found", Response.Status.NOT_FOUND)

        return EventResponse(
            id = record.id,
            title = record.title,
            description = record.description,
            secretVotes = record.secretVotes ?: false,
            participantsCanPoll = record.participantsCanPoll ?: true,
            participantsCanChecklist = record.participantsCanChecklist ?: true,
            participantsCanShoppingList = record.participantsCanShoppingList ?: false,
            stage = EventStage.valueOf(record.stage ?: "SCHEDULING"),
            decidedTimeStart = record.decidedTimeStart,
            decidedTimeEnd = record.decidedTimeEnd,
            createdAt = record.createdAt ?: OffsetDateTime.now(),
        )
    }

    fun getEventWithAdminCheck(
        eventId: UUID,
        adminToken: UUID?,
    ): EventResponse {
        val record =
            dsl
                .selectFrom(EVENTS)
                .where(EVENTS.ID.eq(eventId))
                .fetchOne() ?: throw WebApplicationException("Event not found", Response.Status.NOT_FOUND)

        val isAdmin =
            adminToken != null &&
                MessageDigest.isEqual(
                    record.adminToken.toString().toByteArray(),
                    adminToken.toString().toByteArray(),
                )

        return EventResponse(
            id = record.id,
            title = record.title,
            description = record.description,
            secretVotes = record.secretVotes ?: false,
            participantsCanPoll = record.participantsCanPoll ?: true,
            participantsCanChecklist = record.participantsCanChecklist ?: true,
            participantsCanShoppingList = record.participantsCanShoppingList ?: false,
            stage = EventStage.valueOf(record.stage ?: "SCHEDULING"),
            adminToken = if (isAdmin) record.adminToken else null,
            passphrase = null,
            decidedTimeStart = record.decidedTimeStart,
            decidedTimeEnd = record.decidedTimeEnd,
            createdAt = record.createdAt ?: OffsetDateTime.now(),
        )
    }

    fun updateEvent(
        eventId: UUID,
        adminToken: UUID,
        request: UpdateEventRequest,
    ): EventResponse {
        verifyAdmin(eventId, adminToken)

        val record = dsl.newRecord(EVENTS)
        var hasChanges = false
        if (request.title != null) {
            record.title = request.title
            hasChanges = true
        }
        if (request.description != null) {
            record.description = request.description
            hasChanges = true
        }
        if (request.passphrase != null) {
            record.passphrase = BCrypt.withDefaults().hashToString(12, request.passphrase.toCharArray())
            hasChanges = true
        }
        if (request.participantsCanPoll != null) {
            record.participantsCanPoll = request.participantsCanPoll
            hasChanges = true
        }
        if (request.participantsCanChecklist != null) {
            record.participantsCanChecklist = request.participantsCanChecklist
            hasChanges = true
        }
        if (request.participantsCanShoppingList != null) {
            record.participantsCanShoppingList = request.participantsCanShoppingList
            hasChanges = true
        }
        if (hasChanges) {
            dsl
                .update(EVENTS)
                .set(record)
                .where(EVENTS.ID.eq(eventId))
                .execute()
        }

        return getEventWithAdminCheck(eventId, adminToken)
    }

    fun joinEvent(
        eventId: UUID,
        request: JoinEventRequest,
        userId: UUID? = null,
    ): JoinEventResponse {
        val record =
            dsl
                .selectFrom(EVENTS)
                .where(EVENTS.ID.eq(eventId))
                .fetchOne() ?: throw WebApplicationException("Event not found", Response.Status.NOT_FOUND)

        val passphraseResult = BCrypt.verifyer().verify(request.passphrase.toCharArray(), record.passphrase)
        if (!passphraseResult.verified) {
            throw WebApplicationException("Invalid passphrase", Response.Status.FORBIDDEN)
        }

        val participantId = UUID.randomUUID()

        // Verify user exists before setting foreign key
        val verifiedUserId =
            if (userId != null) {
                val exists = dsl.selectFrom(USERS).where(USERS.ID.eq(userId)).fetchOne()
                if (exists != null) userId else null
            } else {
                null
            }

        dsl
            .insertInto(EVENT_PARTICIPANTS)
            .set(EVENT_PARTICIPANTS.ID, participantId)
            .set(EVENT_PARTICIPANTS.EVENT_ID, eventId)
            .set(EVENT_PARTICIPANTS.USER_ID, verifiedUserId)
            .set(EVENT_PARTICIPANTS.DISPLAY_NAME, request.displayName)
            .set(EVENT_PARTICIPANTS.JOINED_AT, OffsetDateTime.now())
            .execute()

        return JoinEventResponse(
            participantId = participantId,
            displayName = request.displayName,
            eventId = eventId,
        )
    }

    fun decideTime(
        eventId: UUID,
        adminToken: UUID,
        request: DecideTimeRequest,
    ): EventResponse {
        verifyAdmin(eventId, adminToken)

        dsl
            .update(EVENTS)
            .set(EVENTS.DECIDED_TIME_START, request.startTime)
            .set(EVENTS.DECIDED_TIME_END, request.endTime)
            .set(EVENTS.STAGE, EventStage.PLANNING.name)
            .where(EVENTS.ID.eq(eventId))
            .execute()

        return getEventWithAdminCheck(eventId, adminToken)
    }

    fun verifyAdmin(
        eventId: UUID,
        adminToken: UUID,
    ) {
        val record =
            dsl
                .selectFrom(EVENTS)
                .where(EVENTS.ID.eq(eventId))
                .fetchOne() ?: throw WebApplicationException("Event not found", Response.Status.NOT_FOUND)

        if (!MessageDigest.isEqual(
                record.adminToken.toString().toByteArray(),
                adminToken.toString().toByteArray(),
            )
        ) {
            throw WebApplicationException("Invalid admin token", Response.Status.FORBIDDEN)
        }
    }
}
