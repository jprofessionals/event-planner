package com.meet.poll

import com.meet.generated.jooq.tables.PollOptions.Companion.POLL_OPTIONS
import com.meet.generated.jooq.tables.PollVotes.Companion.POLL_VOTES
import com.meet.generated.jooq.tables.Polls.Companion.POLLS
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class PollService(
    private val dsl: DSLContext,
) {
    fun createPoll(
        eventId: UUID,
        request: CreatePollRequest,
    ): PollResponse {
        val pollId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        val options = mutableListOf<PollOptionResponse>()
        dsl.transaction { config ->
            val tx = DSL.using(config)

            tx
                .insertInto(POLLS)
                .set(POLLS.ID, pollId)
                .set(POLLS.EVENT_ID, eventId)
                .set(POLLS.QUESTION, request.question)
                .set(POLLS.ALLOW_MULTIPLE, request.allowMultiple)
                .set(POLLS.CREATED_AT, now)
                .execute()

            request.options.forEach { text ->
                val optionId = UUID.randomUUID()
                tx
                    .insertInto(POLL_OPTIONS)
                    .set(POLL_OPTIONS.ID, optionId)
                    .set(POLL_OPTIONS.POLL_ID, pollId)
                    .set(POLL_OPTIONS.TEXT, text)
                    .execute()

                options.add(
                    PollOptionResponse(
                        id = optionId,
                        text = text,
                        voteCount = 0,
                    ),
                )
            }
        }

        return PollResponse(
            id = pollId,
            question = request.question,
            allowMultiple = request.allowMultiple,
            options = options,
            createdAt = now,
        )
    }

    fun getPolls(eventId: UUID): List<PollResponse> {
        val polls =
            dsl
                .selectFrom(POLLS)
                .where(POLLS.EVENT_ID.eq(eventId))
                .orderBy(POLLS.CREATED_AT.asc())
                .fetch()

        if (polls.isEmpty()) return emptyList()

        val pollIds = polls.map { it.id }

        // Fetch all options for all polls in one query
        val allOptions =
            dsl
                .selectFrom(POLL_OPTIONS)
                .where(POLL_OPTIONS.POLL_ID.`in`(pollIds))
                .fetch()
                .groupBy { it.pollId }

        // Fetch all vote counts for all options in one query
        val allOptionIds = allOptions.values.flatten().map { it.id }
        val voteCounts =
            if (allOptionIds.isNotEmpty()) {
                dsl
                    .select(POLL_VOTES.POLL_OPTION_ID, DSL.count())
                    .from(POLL_VOTES)
                    .where(POLL_VOTES.POLL_OPTION_ID.`in`(allOptionIds))
                    .groupBy(POLL_VOTES.POLL_OPTION_ID)
                    .fetch()
                    .associate { it.value1()!! to it.value2() }
            } else {
                emptyMap()
            }

        return polls.map { poll ->
            val options = allOptions[poll.id] ?: emptyList()
            PollResponse(
                id = poll.id,
                question = poll.question,
                allowMultiple = poll.allowMultiple ?: false,
                options =
                    options.map { option ->
                        PollOptionResponse(
                            id = option.id,
                            text = option.text,
                            voteCount = voteCounts[option.id] ?: 0,
                        )
                    },
                createdAt = poll.createdAt ?: OffsetDateTime.now(),
            )
        }
    }

    fun votePoll(
        eventId: UUID,
        pollId: UUID,
        request: VotePollRequest,
    ) {
        // Verify poll belongs to event
        val poll =
            dsl
                .selectFrom(POLLS)
                .where(POLLS.ID.eq(pollId).and(POLLS.EVENT_ID.eq(eventId)))
                .fetchOne() ?: throw WebApplicationException("Poll not found", Response.Status.NOT_FOUND)

        // Get all option IDs for this poll to delete previous votes
        val pollOptionIds =
            dsl
                .select(POLL_OPTIONS.ID)
                .from(POLL_OPTIONS)
                .where(POLL_OPTIONS.POLL_ID.eq(pollId))
                .fetch(POLL_OPTIONS.ID)

        // Verify submitted optionIds belong to this poll
        val invalidIds = request.optionIds.filter { it !in pollOptionIds }
        if (invalidIds.isNotEmpty()) {
            throw WebApplicationException("Invalid option IDs: $invalidIds", Response.Status.BAD_REQUEST)
        }

        // Enforce allowMultiple constraint
        if (!poll.allowMultiple!! && request.optionIds.size > 1) {
            throw WebApplicationException("This poll does not allow multiple selections", Response.Status.BAD_REQUEST)
        }

        // Delete existing votes and insert new ones atomically
        dsl.transaction { config ->
            val tx = DSL.using(config)

            tx
                .deleteFrom(POLL_VOTES)
                .where(
                    POLL_VOTES.POLL_OPTION_ID
                        .`in`(pollOptionIds)
                        .and(POLL_VOTES.PARTICIPANT_NAME.eq(request.participantName)),
                ).execute()

            request.optionIds.forEach { optionId ->
                val id = UUID.randomUUID()
                tx
                    .insertInto(POLL_VOTES)
                    .set(POLL_VOTES.ID, id)
                    .set(POLL_VOTES.POLL_OPTION_ID, optionId)
                    .set(POLL_VOTES.PARTICIPANT_NAME, request.participantName)
                    .execute()
            }
        }
    }
}
