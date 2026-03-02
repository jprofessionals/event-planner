package com.meet.auth

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class AuthIT {
    @Test
    fun `register creates user and returns token`() {
        val email = "test-${UUID.randomUUID()}@example.com"

        val response =
            given()
                .contentType(ContentType.JSON)
                .body("""{"email":"$email","password":"password123","displayName":"Test User"}""")
                .`when`()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        response.getString("token") shouldNotBe null
        response.getString("user.email") shouldBe email
        response.getString("user.displayName") shouldBe "Test User"
    }

    @Test
    fun `register with duplicate email returns 409`() {
        val email = "dup-${UUID.randomUUID()}@example.com"

        given()
            .contentType(ContentType.JSON)
            .body("""{"email":"$email","password":"password123","displayName":"First User"}""")
            .`when`()
            .post("/api/auth/register")
            .then()
            .statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""{"email":"$email","password":"password123","displayName":"Second User"}""")
            .`when`()
            .post("/api/auth/register")
            .then()
            .statusCode(409)
    }

    @Test
    fun `login with valid credentials returns token`() {
        val email = "login-${UUID.randomUUID()}@example.com"

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
    fun `login with wrong password returns 401`() {
        val email = "wrong-${UUID.randomUUID()}@example.com"

        given()
            .contentType(ContentType.JSON)
            .body("""{"email":"$email","password":"password123","displayName":"Wrong Pass User"}""")
            .`when`()
            .post("/api/auth/register")
            .then()
            .statusCode(201)

        given()
            .contentType(ContentType.JSON)
            .body("""{"email":"$email","password":"wrongpassword"}""")
            .`when`()
            .post("/api/auth/login")
            .then()
            .statusCode(401)
    }

    @Test
    fun `GET me with valid token returns user`() {
        val email = "me-${UUID.randomUUID()}@example.com"

        val registerResponse =
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

        val token = registerResponse.getString("token")

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
    fun `GET me without token returns 401`() {
        given()
            .`when`()
            .get("/api/users/me")
            .then()
            .statusCode(401)
    }

    @Test
    fun `GET me events returns empty list for new user`() {
        val email = "events-${UUID.randomUUID()}@example.com"

        val registerResponse =
            given()
                .contentType(ContentType.JSON)
                .body("""{"email":"$email","password":"password123","displayName":"Events User"}""")
                .`when`()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .jsonPath()

        val token = registerResponse.getString("token")

        val eventsResponse =
            given()
                .header("Authorization", "Bearer $token")
                .`when`()
                .get("/api/users/me/events")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList<Map<String, Any>>("")

        eventsResponse.size shouldBe 0
    }
}
