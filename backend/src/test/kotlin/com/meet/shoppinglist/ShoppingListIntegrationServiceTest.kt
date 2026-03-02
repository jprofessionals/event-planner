package com.meet.shoppinglist

import com.meet.event.CreateEventRequest
import com.meet.event.EventService
import com.meet.generated.jooq.tables.EventShoppingLists.Companion.EVENT_SHOPPING_LISTS
import com.meet.generated.jooq.tables.Events.Companion.EVENTS
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(WireMockShoppingListApi::class)
class ShoppingListIntegrationServiceTest {
    @Inject
    lateinit var service: ShoppingListIntegrationService

    @Inject
    lateinit var eventService: EventService

    @Inject
    lateinit var dsl: DSLContext

    @AfterEach
    fun cleanup() {
        dsl.deleteFrom(EVENT_SHOPPING_LISTS).execute()
        dsl.deleteFrom(EVENTS).execute()
    }

    @Test
    fun `stores shopping list reference after external API call`() {
        val event =
            eventService.createEvent(
                CreateEventRequest(
                    title = "BBQ Party",
                    passphrase = "secret",
                ),
            )

        val result =
            service.createShoppingList(
                eventId = event.id,
                request = CreateShoppingListRequest(title = "BBQ supplies"),
                participantName = "Alice",
            )

        result shouldNotBe null
        result.title shouldBe "BBQ supplies"
        result.createdByParticipant shouldBe "Alice"
    }

    @Test
    fun `lists shopping lists for event`() {
        val event =
            eventService.createEvent(
                CreateEventRequest(
                    title = "Party",
                    passphrase = "secret",
                ),
            )

        service.createShoppingList(
            event.id,
            CreateShoppingListRequest(title = "Drinks"),
            "Alice",
        )
        service.createShoppingList(
            event.id,
            CreateShoppingListRequest(title = "Food"),
            "Bob",
        )

        val lists = service.getShoppingLists(event.id)
        lists.size shouldBe 2
    }

    @Test
    fun `deletes shopping list reference`() {
        val event =
            eventService.createEvent(
                CreateEventRequest(
                    title = "Party",
                    passphrase = "secret",
                ),
            )

        val list =
            service.createShoppingList(
                event.id,
                CreateShoppingListRequest(title = "Drinks"),
                "Alice",
            )

        service.deleteShoppingList(event.id, list.id)

        service.getShoppingLists(event.id).size shouldBe 0
    }
}
