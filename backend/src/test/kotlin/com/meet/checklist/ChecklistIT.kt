package com.meet.checklist

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@QuarkusTest
class ChecklistIT {
    @Inject
    lateinit var dsl: DSLContext

    @AfterEach
    fun cleanup() {
        dsl.deleteFrom(com.meet.generated.jooq.tables.Comments.Companion.COMMENTS).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.ChecklistItems.Companion.CHECKLIST_ITEMS).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.PollVotes.Companion.POLL_VOTES).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.PollOptions.Companion.POLL_OPTIONS).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.Polls.Companion.POLLS).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.EventShoppingLists.Companion.EVENT_SHOPPING_LISTS).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.TimeVotes.Companion.TIME_VOTES).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.TimeOptions.Companion.TIME_OPTIONS).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.EventParticipants.Companion.EVENT_PARTICIPANTS).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.Events.Companion.EVENTS).execute()
        dsl.deleteFrom(com.meet.generated.jooq.tables.Users.Companion.USERS).execute()
    }

    private fun createEventWithPermissions(participantsCanChecklist: Boolean): Pair<String, String> {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Checklist Perms","passphrase":"secret","participantsCanChecklist":$participantsCanChecklist}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        return response.getString("id") to response.getString("adminToken")
    }

    private fun createEvent(): String {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Checklist Test Event","passphrase":"secret"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        return response.getString("id")
    }

    @Test
    fun `add checklist item and GET returns it`() {
        val eventId = createEvent()

        given()
            .contentType(ContentType.JSON)
            .body("""{"text":"Buy snacks","assignedTo":"Alice"}""")
            .`when`()
            .post("/api/events/$eventId/checklist")
            .then()
            .statusCode(201)

        val items =
            given()
                .`when`()
                .get("/api/events/$eventId/checklist")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        items shouldHaveSize 1
        items[0]["text"] shouldBe "Buy snacks"
        items[0]["assignedTo"] shouldBe "Alice"
        items[0]["completed"] shouldBe false
    }

    @Test
    fun `PATCH checklist item updates completed status`() {
        val eventId = createEvent()

        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"text":"Book venue"}""")
                .`when`()
                .post("/api/events/$eventId/checklist")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val itemId = createResponse.getString("id")

        val updateResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"completed":true}""")
                .`when`()
                .patch("/api/events/$eventId/checklist/$itemId")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        updateResponse.getBoolean("completed") shouldBe true
    }

    @Test
    fun `POST checklist item returns 403 when participantsCanChecklist is false and no admin token`() {
        val (eventId, _) = createEventWithPermissions(participantsCanChecklist = false)

        given()
            .contentType(ContentType.JSON)
            .body("""{"text":"Should fail"}""")
            .`when`()
            .post("/api/events/$eventId/checklist")
            .then()
            .statusCode(403)
    }

    @Test
    fun `POST checklist item succeeds with admin token even when participantsCanChecklist is false`() {
        val (eventId, adminToken) = createEventWithPermissions(participantsCanChecklist = false)

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Token", adminToken)
            .body("""{"text":"Admin item"}""")
            .`when`()
            .post("/api/events/$eventId/checklist")
            .then()
            .statusCode(201)
    }
}
