package com.simbirsoft;

import com.github.javafaker.Faker;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReqresTests {

    Faker faker = new Faker();

    @Test
    void successfulRegisterAndGetUser(){
        String userEmail = "emma.wong@reqres.in";

        Integer userId =
                given()
                        .contentType(ContentType.JSON)
                        .body("{\"email\":\"" + userEmail + "\",\"password\": \"pistol\"}")
                        .when()
                        .post("https://reqres.in/api/register")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("id");

        get("https://reqres.in/api/users/" + userId)
                .then()
                .statusCode(200)
                .body("data.email", is(userEmail))
                .body("data.id", is(userId));
    }

    @Test
    void checkUsersCountInList(){
        int usersTotal = 12;
        int usersPerPage = 6;

        List<Object> users =
                get("https://reqres.in/api/users")
                    .then()
                    .statusCode(200)
                    .body("total", is(usersTotal))
                    .body("per_page", is(usersPerPage))
                    .extract()
                    .path("data");

        assertThat(users.size()).isEqualTo(usersPerPage);
    }

    @Test
    void successfulUpdateUser(){
        String userName = faker.name().firstName();
        String userJob = faker.job().position();

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + userName + "\", \"job\":\"" + userJob + "\"}")
                .when()
                .post("https://reqres.in/api/users/2")
                .then()
                .statusCode(201)
                .body("name", is(userName))
                .body("job", is(userJob));
    }

    @Test
    void checkUserInList(){
        int userId = faker.number().numberBetween(1, 6);

        Object user =
                get("https://reqres.in/api/users/" + userId)
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("data");

        List<Object> users =
                get("https://reqres.in/api/users")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("data");

        assertTrue(users.contains(user), "User not found in list");
    }

    @Test
    void unsuccessfulRegisterUser(){
        String userEmail = faker.internet().emailAddress();
        String userPassword = faker.internet().password();

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"" + userEmail + "\", \"password\": \"" + userPassword + "\"}")
                .when()
                .post("https://reqres.in/api/register")
                .then()
                .statusCode(400)
                .body("error", is("Note: Only defined users succeed registration"));
    }
}
