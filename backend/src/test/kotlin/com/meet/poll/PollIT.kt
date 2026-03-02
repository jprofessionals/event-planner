package com.meet.poll

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@QuarkusTest
class PollIT {
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

    private fun createEventAndGetAdminToken(): Pair<String, String> {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Poll Test Event","passphrase":"secret"}""")
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
    fun `admin creates poll and GET returns it`() {
        val (eventId, adminToken) = createEventAndGetAdminToken()

        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"question":"What food?","options":["Pizza","Sushi","Tacos"],"allowMultiple":false}""")
                .`when`()
                .post("/api/events/$eventId/polls")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        createResponse.getString("question") shouldBe "What food?"
        createResponse.getList<Map<String, Any>>("options") shouldHaveSize 3

        val polls =
            given()
                .`when`()
                .get("/api/events/$eventId/polls")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        polls shouldHaveSize 1
    }

    @Test
    fun `participant votes on poll and vote count increases`() {
        val (eventId, adminToken) = createEventAndGetAdminToken()

        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"question":"Drink?","options":["Water","Juice"],"allowMultiple":false}""")
                .`when`()
                .post("/api/events/$eventId/polls")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val pollId = createResponse.getString("id")
        val optionId = createResponse.getList<Map<String, Any>>("options")[0]["id"]

        given()
            .contentType(ContentType.JSON)
            .body("""{"participantName":"Alice","optionIds":["$optionId"]}""")
            .`when`()
            .post("/api/events/$eventId/polls/$pollId/vote")
            .then()
            .statusCode(200)

        val polls =
            given()
                .`when`()
                .get("/api/events/$eventId/polls")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        val options = polls.getList<Map<String, Any>>("[0].options")
        val votedOption = options.find { it["id"].toString() == optionId.toString() }
        votedOption shouldNotBe null
        (votedOption!!["voteCount"] as Int) shouldBe 1
    }

    @Test
    fun `POST poll returns 403 when participantsCanPoll is false and no admin token`() {
        val eventResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"No Polls","passphrase":"secret","participantsCanPoll":false}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = eventResponse.getString("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"question":"Should fail","options":["A","B"],"allowMultiple":false}""")
            .`when`()
            .post("/api/events/$eventId/polls")
            .then()
            .statusCode(403)
    }

    @Test
    fun `POST poll succeeds with admin token even when participantsCanPoll is false`() {
        val eventResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Admin Polls","passphrase":"secret","participantsCanPoll":false}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = eventResponse.getString("id")
        val adminToken = eventResponse.getString("adminToken")

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Token", adminToken)
            .body("""{"question":"Admin poll","options":["A","B"],"allowMultiple":false}""")
            .`when`()
            .post("/api/events/$eventId/polls")
            .then()
            .statusCode(201)
    }
}
