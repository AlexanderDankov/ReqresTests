package com.simbirsoft.tests;

import com.github.javafaker.Faker;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.simbirsoft.filters.CustomLogFilter.customLogFilter;
import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReqresTests {

    Faker faker = new Faker();
    private static Integer userId;
    private static final String userEmail = "emma.wong@reqres.in";
    private static final int
            usersTotal = 12,
            usersPerPage = 6;
    private static List<Object> users;
    private static Object user;

    @Test
    @Feature("Authorization")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Успешная регистрация пользователя и получение его данных")
    void successfulRegisterAndGetUser(){
        step("Регистрация пользователя", () ->
                userId =
                        given()
                                .filter(customLogFilter().withCustomTemplates())
                                .log().uri()
                                .log().body()
                                .contentType(ContentType.JSON)
                                .body("{\"email\":\"" + userEmail + "\",\"password\": \"pistol\"}")
                                .when()
                                .post("https://reqres.in/api/register")
                                .then()
                                .log().body()
                                .statusCode(200)
                                .body(matchesJsonSchemaInClasspath("schemas/UserRegistrationSchema.json"))
                                .extract()
                                .path("id")

        );

        step("Получение данных по зарегистрированному пользователю", () ->
                given()
                        .filter(customLogFilter().withCustomTemplates())
                        .log().uri()
                        .when()
                        .get("https://reqres.in/api/users/" + userId)
                        .then()
                        .log().body()
                        .statusCode(200)
                        .body(matchesJsonSchemaInClasspath("schemas/GetUserSchema.json"))
                        .body("data.email", is(userEmail))
                        .body("data.id", is(userId))
        );
    }

    @Test
    @Feature("Users List")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Проверка количества пользователей")
    void checkUsersCountInList(){
        step("Получение списка пользователей", () ->
                users =
                        given()
                                .filter(customLogFilter().withCustomTemplates())
                                .log().uri()
                                .when()
                                .get("https://reqres.in/api/users")
                                .then()
                                .log().body()
                                .statusCode(200)
                                .body(matchesJsonSchemaInClasspath("schemas/ListUsersSchema.json"))
                                .body("total", is(usersTotal))
                                .body("per_page", is(usersPerPage))
                                .extract()
                                .path("data")
        );

        step("Проверка количества выводимых пользователей в ответе", () ->
                assertThat(users.size()).isEqualTo(usersPerPage)
        );
    }

    @Test
    @Feature("User profile")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Успешное обновление данных пользователя")
    void successfulUpdateUser(){
        String userName = faker.name().firstName();
        String userJob = faker.job().position();

        step("Обновление данных профиля пользователя", () ->
                given()
                        .filter(customLogFilter().withCustomTemplates())
                        .log().uri()
                        .log().body()
                        .contentType(ContentType.JSON)
                        .body("{\"name\": \"" + userName + "\", \"job\":\"" + userJob + "\"}")
                        .when()
                        .post("https://reqres.in/api/users/2")
                        .then()
                        .log().body()
                        .statusCode(201)
                        .body(matchesJsonSchemaInClasspath("schemas/UpdateUserSchema.json"))
                        .body("name", is(userName))
                        .body("job", is(userJob))
        );
    }

    @Test
    @Feature("Users List")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Проверка наличия пользователя в общем списке")
    void checkUserInList(){
        int userId = faker.number().numberBetween(1, 6);

        step("Получение данных по пользователю с id " + userId, () ->
                user =
                        given()
                                .filter(customLogFilter().withCustomTemplates())
                                .log().uri()
                                .when()
                                .get("https://reqres.in/api/users/" + userId)
                                .then()
                                .log().body()
                                .statusCode(200)
                                .body(matchesJsonSchemaInClasspath("schemas/GetUserSchema.json"))
                                .extract()
                                .path("data")
        );

        step("Получение списка пользователей", () ->
                users =
                        given()
                                .filter(customLogFilter().withCustomTemplates())
                                .log().uri()
                                .when()
                                .get("https://reqres.in/api/users")
                                .then()
                                .log().body()
                                .statusCode(200)
                                .body(matchesJsonSchemaInClasspath("schemas/ListUsersSchema.json"))
                                .extract()
                                .path("data")
        );

        step("Проверка наличия пользователя с id " + userId + " в списке пользователей", () ->
                assertTrue(users.contains(user), "User not found in list")
        );
    }

    @Test
    @Feature("Registration")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Проверка сообщения об ошибке при попытке регистрации стороннего пользователя")
    void unsuccessfulRegisterUser(){
        String randomUserEmail = faker.internet().emailAddress();
        String randomUserPassword = faker.internet().password();

        step("Проверка сообщения об ошибке при регистрации стороннего пользователя", () ->
                given()
                        .filter(customLogFilter().withCustomTemplates())
                        .log().uri()
                        .log().body()
                        .contentType(ContentType.JSON)
                        .body("{\"email\": \"" + randomUserEmail + "\", \"password\": \"" + randomUserPassword + "\"}")
                        .when()
                        .post("https://reqres.in/api/register")
                        .then()
                        .log().body()
                        .statusCode(400)
                        .body("error", is("Note: Only defined users succeed registration"))
        );
    }
}
