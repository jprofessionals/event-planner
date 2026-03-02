package com.meet.scheduling

import com.meet.event.EventService
import com.meet.generated.jooq.tables.Events.Companion.EVENTS
import com.meet.generated.jooq.tables.TimeOptions.Companion.TIME_OPTIONS
import com.meet.generated.jooq.tables.TimeVotes.Companion.TIME_VOTES
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.util.UUID

@ApplicationScoped
class SchedulingService(
    private val dsl: DSLContext,
    private val eventService: EventService,
) {
    fun addTimeOptions(
        eventId: UUID,
        adminToken: UUID,
        request: AddTimeOptionsRequest,
    ): List<TimeOptionResponse> {
        eventService.verifyAdmin(eventId, adminToken)

        return request.options.map { option ->
            val id = UUID.randomUUID()
            dsl
                .insertInto(TIME_OPTIONS)
                .set(TIME_OPTIONS.ID, id)
                .set(TIME_OPTIONS.EVENT_ID, eventId)
                .set(TIME_OPTIONS.START_TIME, option.startTime)
                .set(TIME_OPTIONS.END_TIME, option.endTime)
                .execute()

            TimeOptionResponse(
                id = id,
                startTime = option.startTime,
                endTime = option.endTime,
            )
        }
    }

    fun getTimeOptions(
        eventId: UUID,
        participantName: String? = null,
        isAdmin: Boolean = false,
    ): List<TimeOptionResponse> {
        val options =
            dsl
                .selectFrom(TIME_OPTIONS)
                .where(TIME_OPTIONS.EVENT_ID.eq(eventId))
                .orderBy(TIME_OPTIONS.START_TIME.asc())
                .fetch()

        val optionIds = options.map { it.id }

        val votes =
            if (optionIds.isNotEmpty()) {
                dsl
                    .selectFrom(TIME_VOTES)
                    .where(TIME_VOTES.TIME_OPTION_ID.`in`(optionIds))
                    .fetch()
                    .groupBy { it.timeOptionId }
            } else {
                emptyMap()
            }

        // Check if secret votes are enabled for this event
        val secretVotes =
            dsl
                .select(EVENTS.SECRET_VOTES)
                .from(EVENTS)
                .where(EVENTS.ID.eq(eventId))
                .fetchOne(EVENTS.SECRET_VOTES) ?: false

        return options.map { option ->
            val optionVotes = votes[option.id] ?: emptyList()
            val filteredVotes =
                if (secretVotes && !isAdmin) {
                    optionVotes.filter { it.participantName == participantName }
                } else {
                    optionVotes
                }
            TimeOptionResponse(
                id = option.id,
                startTime = option.startTime,
                endTime = option.endTime,
                votes =
                    filteredVotes.map { vote ->
                        VoteResponse(
                            id = vote.id,
                            participantName = vote.participantName,
                            vote = VoteType.valueOf(vote.vote),
                        )
                    },
            )
        }
    }

    fun castVotes(
        eventId: UUID,
        request: CastVotesRequest,
    ): List<VoteResponse> {
        val results = mutableListOf<VoteResponse>()
        dsl.transaction { config ->
            val tx = DSL.using(config)
            request.votes.forEach { voteInput ->
                if (voteInput.vote == VoteType.NONE) {
                    tx
                        .deleteFrom(TIME_VOTES)
                        .where(TIME_VOTES.TIME_OPTION_ID.eq(voteInput.timeOptionId))
                        .and(TIME_VOTES.PARTICIPANT_NAME.eq(request.participantName))
                        .execute()
                } else {
                    val id = UUID.randomUUID()
                    tx
                        .insertInto(TIME_VOTES)
                        .set(TIME_VOTES.ID, id)
                        .set(TIME_VOTES.TIME_OPTION_ID, voteInput.timeOptionId)
                        .set(TIME_VOTES.PARTICIPANT_NAME, request.participantName)
                        .set(TIME_VOTES.VOTE, voteInput.vote.name)
                        .onConflict(TIME_VOTES.TIME_OPTION_ID, TIME_VOTES.PARTICIPANT_NAME)
                        .doUpdate()
                        .set(TIME_VOTES.VOTE, voteInput.vote.name)
                        .execute()

                    results.add(
                        VoteResponse(
                            id = id,
                            participantName = request.participantName,
                            vote = voteInput.vote,
                        ),
                    )
                }
            }
        }
        return results
    }

    fun deleteTimeOption(
        eventId: UUID,
        adminToken: UUID,
        optionId: UUID,
    ) {
        eventService.verifyAdmin(eventId, adminToken)

        val deleted =
            dsl
                .deleteFrom(TIME_OPTIONS)
                .where(TIME_OPTIONS.ID.eq(optionId).and(TIME_OPTIONS.EVENT_ID.eq(eventId)))
                .execute()

        if (deleted == 0) {
            throw WebApplicationException("Time option not found", Response.Status.NOT_FOUND)
        }
    }
}
