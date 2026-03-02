package com.meet.comment

import com.meet.generated.jooq.tables.Comments.Companion.COMMENTS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class CommentService(
    private val dsl: DSLContext,
) {
    fun addComment(
        eventId: UUID,
        request: AddCommentRequest,
    ): CommentResponse {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()

        dsl
            .insertInto(COMMENTS)
            .set(COMMENTS.ID, id)
            .set(COMMENTS.EVENT_ID, eventId)
            .set(COMMENTS.AUTHOR_NAME, request.authorName)
            .set(COMMENTS.CONTENT, request.content)
            .set(COMMENTS.CREATED_AT, now)
            .execute()

        return CommentResponse(
            id = id,
            authorName = request.authorName,
            content = request.content,
            createdAt = now,
        )
    }

    fun getComments(eventId: UUID): List<CommentResponse> =
        dsl
            .selectFrom(COMMENTS)
            .where(COMMENTS.EVENT_ID.eq(eventId))
            .orderBy(COMMENTS.CREATED_AT.asc())
            .fetch()
            .map { record ->
                CommentResponse(
                    id = record.id,
                    authorName = record.authorName,
                    content = record.content,
                    createdAt = record.createdAt ?: OffsetDateTime.now(),
                )
            }
}
