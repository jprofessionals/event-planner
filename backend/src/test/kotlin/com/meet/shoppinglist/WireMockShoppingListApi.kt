package com.meet.shoppinglist

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager

class WireMockShoppingListApi : QuarkusTestResourceLifecycleManager {
    private lateinit var wireMock: WireMockServer

    override fun start(): Map<String, String> {
        wireMock = WireMockServer(wireMockConfig().dynamicPort())
        wireMock.start()
        wireMock.stubFor(
            post(urlEqualTo("/api/external/lists"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "listId": "ext-list-123",
                                "shareToken": "abc123def456ghi789jkl012mno345pq",
                                "widgetUrl": "/widget/abc123def456ghi789jkl012mno345pq"
                            }
                            """.trimIndent(),
                        ),
                ),
        )
        return mapOf("shopping-list.api-url" to "http://localhost:${wireMock.port()}")
    }

    override fun stop() {
        wireMock.stop()
    }
}
