package com.meet.scheduling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@QuarkusTest
class SchedulingIT {
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

    private fun createEventAndGetAdminToken(secretVotes: Boolean = false): Pair<String, String> {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Scheduling Test","passphrase":"secret","secretVotes":$secretVotes}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        return response.getString("id") to response.getString("adminToken")
    }

    @Test
    fun `admin adds time options and GET returns them`() {
        val (eventId, adminToken) = createEventAndGetAdminToken()
        val now = OffsetDateTime.now()

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Token", adminToken)
            .body(
                """{"options":[{"startTime":"$now","endTime":"${now.plusHours(
                    1,
                )}"},{"startTime":"${now.plusHours(2)}","endTime":"${now.plusHours(3)}"}]}""",
            ).`when`()
            .post("/api/events/$eventId/time-options")
            .then()
            .statusCode(201)

        val options =
            given()
                .`when`()
                .get("/api/events/$eventId/time-options")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        options shouldHaveSize 2
    }

    @Test
    fun `participant casts votes and GET shows votes on options`() {
        val (eventId, adminToken) = createEventAndGetAdminToken()
        val now = OffsetDateTime.now()

        // Add time options
        val addedOptions =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"options":[{"startTime":"$now","endTime":"${now.plusHours(1)}"}]}""")
                .`when`()
                .post("/api/events/$eventId/time-options")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        val optionId = addedOptions[0]["id"]

        // Cast votes
        given()
            .contentType(ContentType.JSON)
            .body("""{"participantName":"Alice","votes":[{"timeOptionId":"$optionId","vote":"YES"}]}""")
            .`when`()
            .post("/api/events/$eventId/votes")
            .then()
            .statusCode(200)

        // Verify votes appear on GET
        val options =
            given()
                .`when`()
                .get("/api/events/$eventId/time-options")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        val votes = options.getList<Map<String, Any>>("[0].votes")
        votes shouldHaveSize 1
        votes[0]["participantName"] shouldBe "Alice"
        votes[0]["vote"] shouldBe "YES"
    }

    @Test
    fun `voting twice on same option overwrites previous vote`() {
        val (eventId, adminToken) = createEventAndGetAdminToken()
        val now = OffsetDateTime.now()

        // Add time option
        val addedOptions =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"options":[{"startTime":"$now","endTime":"${now.plusHours(1)}"}]}""")
                .`when`()
                .post("/api/events/$eventId/time-options")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        val optionId = addedOptions[0]["id"]

        // Vote YES first
        given()
            .contentType(ContentType.JSON)
            .body("""{"participantName":"Alice","votes":[{"timeOptionId":"$optionId","vote":"YES"}]}""")
            .`when`()
            .post("/api/events/$eventId/votes")
            .then()
            .statusCode(200)

        // Vote MAYBE second (should overwrite)
        given()
            .contentType(ContentType.JSON)
            .body("""{"participantName":"Alice","votes":[{"timeOptionId":"$optionId","vote":"MAYBE"}]}""")
            .`when`()
            .post("/api/events/$eventId/votes")
            .then()
            .statusCode(200)

        // Verify only 1 vote with MAYBE
        val options =
            given()
                .`when`()
                .get("/api/events/$eventId/time-options")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        val votes = options.getList<Map<String, Any>>("[0].votes")
        votes shouldHaveSize 1
        votes[0]["participantName"] shouldBe "Alice"
        votes[0]["vote"] shouldBe "MAYBE"
    }

    @Test
    fun `admin can delete time option`() {
        val (eventId, adminToken) = createEventAndGetAdminToken()
        val now = OffsetDateTime.now()

        // Add time option
        val addedOptions =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"options":[{"startTime":"$now","endTime":"${now.plusHours(1)}"}]}""")
                .`when`()
                .post("/api/events/$eventId/time-options")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        val optionId = addedOptions[0]["id"]

        // Delete time option
        given()
            .header("X-Admin-Token", adminToken)
            .`when`()
            .delete("/api/events/$eventId/time-options/$optionId")
            .then()
            .statusCode(204)

        // Verify GET returns 0 options
        val options =
            given()
                .`when`()
                .get("/api/events/$eventId/time-options")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        options shouldHaveSize 0
    }

    @Test
    fun `secret votes hides other participants votes`() {
        val (eventId, adminToken) = createEventAndGetAdminToken(secretVotes = true)
        val now = OffsetDateTime.now()

        // Add time option
        val addedOptions =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"options":[{"startTime":"$now","endTime":"${now.plusHours(1)}"}]}""")
                .`when`()
                .post("/api/events/$eventId/time-options")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        val optionId = addedOptions[0]["id"]

        // Alice votes
        given()
            .contentType(ContentType.JSON)
            .body("""{"participantName":"Alice","votes":[{"timeOptionId":"$optionId","vote":"YES"}]}""")
            .`when`()
            .post("/api/events/$eventId/votes")
            .then()
            .statusCode(200)

        // Bob votes
        given()
            .contentType(ContentType.JSON)
            .body("""{"participantName":"Bob","votes":[{"timeOptionId":"$optionId","vote":"NO"}]}""")
            .`when`()
            .post("/api/events/$eventId/votes")
            .then()
            .statusCode(200)

        // Alice should only see her own vote
        val aliceOptions =
            given()
                .header("X-Participant-Name", "Alice")
                .`when`()
                .get("/api/events/$eventId/time-options")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        val aliceVotes = aliceOptions.getList<Map<String, Any>>("[0].votes")
        aliceVotes shouldHaveSize 1
        aliceVotes[0]["participantName"] shouldBe "Alice"
        aliceVotes[0]["vote"] shouldBe "YES"

        // Admin should see both votes
        val adminOptions =
            given()
                .header("X-Admin-Token", adminToken)
                .`when`()
                .get("/api/events/$eventId/time-options")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        val adminVotes = adminOptions.getList<Map<String, Any>>("[0].votes")
        adminVotes shouldHaveSize 2
    }
}
