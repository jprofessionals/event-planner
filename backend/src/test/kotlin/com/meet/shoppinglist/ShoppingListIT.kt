package com.meet.shoppinglist

import com.meet.event.CreateEventRequest
import com.meet.event.EventService
import com.meet.generated.jooq.tables.EventShoppingLists.Companion.EVENT_SHOPPING_LISTS
import com.meet.generated.jooq.tables.Events.Companion.EVENTS
import io.kotest.matchers.shouldBe
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(WireMockShoppingListApi::class)
class ShoppingListIT {
    @Inject
    lateinit var eventService: EventService

    @Inject
    lateinit var dsl: DSLContext

    @AfterEach
    fun cleanup() {
        dsl.deleteFrom(EVENT_SHOPPING_LISTS).execute()
        dsl.deleteFrom(EVENTS).execute()
    }

    private fun createEventAndGetAdminToken(): Pair<String, String> {
        val event =
            eventService.createEvent(
                CreateEventRequest(
                    title = "Test Event",
                    passphrase = "secret",
                ),
            )
        return event.id.toString() to event.adminToken.toString()
    }

    @Test
    fun `admin creates shopping list`() {
        val (eventId, adminToken) = createEventAndGetAdminToken()

        val response =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"title":"Party supplies"}""")
                .`when`()
                .post("/api/events/$eventId/shopping-lists")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        response.getString("title") shouldBe "Party supplies"
        response.getString("shareToken").length shouldBe 32
    }

    @Test
    fun `participant without permission gets 403`() {
        val (eventId, _) = createEventAndGetAdminToken()

        given()
            .contentType(ContentType.JSON)
            .body("""{"title":"Snacks"}""")
            .`when`()
            .post("/api/events/$eventId/shopping-lists")
            .then()
            .statusCode(403)
    }

    @Test
    fun `GET returns shopping lists for event`() {
        val (eventId, adminToken) = createEventAndGetAdminToken()

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Token", adminToken)
            .body("""{"title":"Drinks"}""")
            .`when`()
            .post("/api/events/$eventId/shopping-lists")
            .then()
            .statusCode(201)

        val lists =
            given()
                .`when`()
                .get("/api/events/$eventId/shopping-lists")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        lists.size shouldBe 1
    }

    @Test
    fun `admin deletes shopping list`() {
        val (eventId, adminToken) = createEventAndGetAdminToken()

        val listId =
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Token", adminToken)
                .body("""{"title":"Drinks"}""")
                .`when`()
                .post("/api/events/$eventId/shopping-lists")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()
                .getString("id")

        given()
            .header("X-Admin-Token", adminToken)
            .`when`()
            .delete("/api/events/$eventId/shopping-lists/$listId")
            .then()
            .statusCode(204)
    }
}
