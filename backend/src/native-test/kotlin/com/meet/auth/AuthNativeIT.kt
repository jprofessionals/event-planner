package com.meet.auth

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.quarkus.test.junit.QuarkusIntegrationTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Runs against the native binary to catch GraalVM reflection/serialization issues.
 *
 * Run with: ./gradlew testNative
 * (This builds the native image first, then starts it and runs these tests)
 */
@QuarkusIntegrationTest
class AuthNativeIT {
    @Test
    fun `register creates user and returns token`() {
        val email = "native-${UUID.randomUUID()}@example.com"

        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"email":"$email","password":"password123","displayName":"Native Test User"}""")
                .`when`()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        response.getString("token") shouldNotBe null
        response.getString("user.email") shouldBe email
        response.getString("user.displayName") shouldBe "Native Test User"
    }

    @Test
    fun `login with valid credentials returns token`() {
        val email = "native-login-${UUID.randomUUID()}@example.com"

        given()
            .contentType(ContentType.JSON)
            .body("""{"email":"$email","password":"password123","displayName":"Login User"}""")
            .`when`()
            .post("/api/auth/register")
            .then()
            .statusCode(201)

        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"email":"$email","password":"password123"}""")
                .`when`()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        response.getString("token") shouldNotBe null
        response.getString("user.email") shouldBe email
    }

    @Test
    fun `token from register works for authenticated endpoint`() {
        val email = "native-me-${UUID.randomUUID()}@example.com"

        val token =
            given()
                .contentType(ContentType.JSON)
                .body("""{"email":"$email","password":"password123","displayName":"Me User"}""")
                .`when`()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()
                .getString("token")

        val meResponse =
            given()
                .header("Authorization", "Bearer $token")
                .`when`()
                .get("/api/users/me")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        meResponse.getString("email") shouldBe email
        meResponse.getString("displayName") shouldBe "Me User"
    }

    @Test
    fun `token from login works for authenticated endpoint`() {
        val email = "native-login-me-${UUID.randomUUID()}@example.com"

        given()
            .contentType(ContentType.JSON)
            .body("""{"email":"$email","password":"password123","displayName":"Login Me User"}""")
            .`when`()
            .post("/api/auth/register")
            .then()
            .statusCode(201)

        val token =
            given()
                .contentType(ContentType.JSON)
                .body("""{"email":"$email","password":"password123"}""")
                .`when`()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getString("token")

        val meResponse =
            given()
                .header("Authorization", "Bearer $token")
                .`when`()
                .get("/api/users/me")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()

        meResponse.getString("email") shouldBe email
        meResponse.getString("displayName") shouldBe "Login Me User"
    }
}
