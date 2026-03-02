package com.meet.checklist

import com.meet.generated.jooq.tables.ChecklistItems.Companion.CHECKLIST_ITEMS
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class ChecklistService(
    private val dsl: DSLContext,
) {
    fun addItem(
        eventId: UUID,
        request: AddChecklistItemRequest,
    ): ChecklistItemResponse {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()

        dsl
            .insertInto(CHECKLIST_ITEMS)
            .set(CHECKLIST_ITEMS.ID, id)
            .set(CHECKLIST_ITEMS.EVENT_ID, eventId)
            .set(CHECKLIST_ITEMS.TEXT, request.text)
            .set(CHECKLIST_ITEMS.COMPLETED, false)
            .set(CHECKLIST_ITEMS.ASSIGNED_TO, request.assignedTo)
            .set(CHECKLIST_ITEMS.CREATED_AT, now)
            .execute()

        return ChecklistItemResponse(
            id = id,
            text = request.text,
            completed = false,
            assignedTo = request.assignedTo,
            createdAt = now,
        )
    }

    fun getItems(eventId: UUID): List<ChecklistItemResponse> =
        dsl
            .selectFrom(CHECKLIST_ITEMS)
            .where(CHECKLIST_ITEMS.EVENT_ID.eq(eventId))
            .orderBy(CHECKLIST_ITEMS.CREATED_AT.asc())
            .fetch()
            .map { record ->
                ChecklistItemResponse(
                    id = record.id,
                    text = record.text,
                    completed = record.completed ?: false,
                    assignedTo = record.assignedTo,
                    createdAt = record.createdAt ?: OffsetDateTime.now(),
                )
            }

    fun updateItem(
        eventId: UUID,
        itemId: UUID,
        request: UpdateChecklistItemRequest,
    ): ChecklistItemResponse {
        dsl
            .selectFrom(CHECKLIST_ITEMS)
            .where(CHECKLIST_ITEMS.ID.eq(itemId).and(CHECKLIST_ITEMS.EVENT_ID.eq(eventId)))
            .fetchOne() ?: throw WebApplicationException("Checklist item not found", Response.Status.NOT_FOUND)

        if (request.completed != null) {
            dsl
                .update(CHECKLIST_ITEMS)
                .set(CHECKLIST_ITEMS.COMPLETED, request.completed)
                .apply {
                    if (request.text != null) set(CHECKLIST_ITEMS.TEXT, request.text)
                    if (request.assignedTo != null) set(CHECKLIST_ITEMS.ASSIGNED_TO, request.assignedTo)
                }.where(CHECKLIST_ITEMS.ID.eq(itemId))
                .execute()
        } else if (request.text != null) {
            dsl
                .update(CHECKLIST_ITEMS)
                .set(CHECKLIST_ITEMS.TEXT, request.text)
                .apply {
                    if (request.assignedTo != null) set(CHECKLIST_ITEMS.ASSIGNED_TO, request.assignedTo)
                }.where(CHECKLIST_ITEMS.ID.eq(itemId))
                .execute()
        } else if (request.assignedTo != null) {
            dsl
                .update(CHECKLIST_ITEMS)
                .set(CHECKLIST_ITEMS.ASSIGNED_TO, request.assignedTo)
                .where(CHECKLIST_ITEMS.ID.eq(itemId))
                .execute()
        }

        val updated =
            dsl
                .selectFrom(CHECKLIST_ITEMS)
                .where(CHECKLIST_ITEMS.ID.eq(itemId))
                .fetchOne()!!

        return ChecklistItemResponse(
            id = updated.id,
            text = updated.text,
            completed = updated.completed ?: false,
            assignedTo = updated.assignedTo,
            createdAt = updated.createdAt ?: OffsetDateTime.now(),
        )
    }
}
