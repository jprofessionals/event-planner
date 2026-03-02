package com.meet.comment

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
class CommentIT {
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

    private fun createEvent(): String {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Comment Test Event","passphrase":"secret"}""")
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
    fun `add comment and GET returns it`() {
        val eventId = createEvent()

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorName":"Alice","content":"Looking forward to this!"}""")
            .`when`()
            .post("/api/events/$eventId/comments")
            .then()
            .statusCode(201)

        val comments =
            given()
                .`when`()
                .get("/api/events/$eventId/comments")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        comments shouldHaveSize 1
        comments[0]["authorName"] shouldBe "Alice"
        comments[0]["content"] shouldBe "Looking forward to this!"
    }

    @Test
    fun `multiple comments are returned in order`() {
        val eventId = createEvent()

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorName":"Alice","content":"First comment"}""")
            .`when`()
            .post("/api/events/$eventId/comments")
            .then()
            .statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorName":"Bob","content":"Second comment"}""")
            .`when`()
            .post("/api/events/$eventId/comments")
            .then()
            .statusCode(201)

        val comments =
            given()
                .`when`()
                .get("/api/events/$eventId/comments")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        comments shouldHaveSize 2
        comments[0]["authorName"] shouldBe "Alice"
        comments[1]["authorName"] shouldBe "Bob"
    }
}
