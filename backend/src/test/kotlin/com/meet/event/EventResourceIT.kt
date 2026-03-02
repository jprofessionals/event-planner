package com.meet.event

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

@QuarkusTest
class EventResourceIT {
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

    @Test
    fun `POST events creates event and returns id and adminToken`() {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Test Event","passphrase":"secret123"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        response.getString("id") shouldNotBe null
        response.getString("adminToken") shouldNotBe null
        response.getString("title") shouldBe "Test Event"
        response.getString("stage") shouldBe "SCHEDULING"
    }

    @Test
    fun `GET events by id returns event without adminToken`() {
        // Create event first
        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Get Test Event","passphrase":"secret123"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = createResponse.getString("id")

        // Get event without admin token
        val getResponse =
            given()
                .`when`()
                .get("/api/events/$eventId")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        getResponse.getString("title") shouldBe "Get Test Event"
        getResponse.getString("adminToken") shouldBe null
    }

    @Test
    fun `GET events by id with valid X-Admin-Token includes adminToken`() {
        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Admin Check Event","passphrase":"secret123"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = createResponse.getString("id")
        val adminToken = createResponse.getString("adminToken")

        val getResponse =
            given()
                .header("X-Admin-Token", adminToken)
                .`when`()
                .get("/api/events/$eventId")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        getResponse.getString("adminToken") shouldBe adminToken
    }

    @Test
    fun `PATCH events with valid admin token updates title`() {
        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Original Title","passphrase":"secret123"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = createResponse.getString("id")
        val adminToken = createResponse.getString("adminToken")

        val updateResponse =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"title":"Updated Title"}""")
                .`when`()
                .patch("/api/events/$eventId")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        updateResponse.getString("title") shouldBe "Updated Title"
    }

    @Test
    fun `PATCH events with invalid admin token returns 403`() {
        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Forbidden Test","passphrase":"secret123"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = createResponse.getString("id")

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Token", UUID.randomUUID().toString())
            .body("""{"title":"Should Fail"}""")
            .`when`()
            .patch("/api/events/$eventId")
            .then()
            .statusCode(403)
    }

    @Test
    fun `POST join with correct passphrase succeeds`() {
        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Join Test Event","passphrase":"joinme"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = createResponse.getString("id")

        val joinResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"passphrase":"joinme","displayName":"Alice"}""")
                .`when`()
                .post("/api/events/$eventId/join")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        joinResponse.getString("displayName") shouldBe "Alice"
        joinResponse.getString("participantId") shouldNotBe null
    }

    @Test
    fun `POST join with wrong passphrase returns 403`() {
        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Wrong Pass Event","passphrase":"correct"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = createResponse.getString("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"passphrase":"wrong","displayName":"Bob"}""")
            .`when`()
            .post("/api/events/$eventId/join")
            .then()
            .statusCode(403)
    }

    @Test
    fun `POST decide transitions stage to PLANNING`() {
        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Decide Test Event","passphrase":"secret123"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = createResponse.getString("id")
        val adminToken = createResponse.getString("adminToken")

        val now = OffsetDateTime.now()
        val decideResponse =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"startTime":"$now","endTime":"${now.plusHours(2)}"}""")
                .`when`()
                .post("/api/events/$eventId/decide")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        decideResponse.getString("stage") shouldBe "PLANNING"
        decideResponse.getString("decidedTimeStart") shouldNotBe null
        decideResponse.getString("decidedTimeEnd") shouldNotBe null
    }

    @Test
    fun `POST events returns default participant permissions as true`() {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Perms Test","passphrase":"secret"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        response.getBoolean("participantsCanPoll") shouldBe true
        response.getBoolean("participantsCanChecklist") shouldBe true
    }

    @Test
    fun `POST events with permissions disabled returns false`() {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Locked Event","passphrase":"secret","participantsCanPoll":false,"participantsCanChecklist":false}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        response.getBoolean("participantsCanPoll") shouldBe false
        response.getBoolean("participantsCanChecklist") shouldBe false
    }

    @Test
    fun `PATCH events updates participant permissions`() {
        val createResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"title":"Update Perms","passphrase":"secret"}""")
                .`when`()
                .post("/api/events")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val eventId = createResponse.getString("id")
        val adminToken = createResponse.getString("adminToken")

        val updateResponse =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"participantsCanPoll":false,"participantsCanChecklist":false}""")
                .`when`()
                .patch("/api/events/$eventId")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        updateResponse.getBoolean("participantsCanPoll") shouldBe false
        updateResponse.getBoolean("participantsCanChecklist") shouldBe false
    }
}
