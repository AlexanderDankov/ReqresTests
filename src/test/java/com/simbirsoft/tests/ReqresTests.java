package com.simbirsoft.tests;

import com.github.javafaker.Faker;
import com.simbirsoft.models.UpdateUser;
import com.simbirsoft.models.UserData;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.simbirsoft.specs.Specs.request;
import static com.simbirsoft.specs.Specs.responseSpec;
import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

public class ReqresTests {

    Faker faker = new Faker();
    private static Integer userId;
    private static final String
            userEmail = "emma.wong@reqres.in",
            firstName = "Emma",
            lastName = "Wong";
    private static final int
            usersTotal = 12,
            usersPerPage = 6;
    private static List<Object> users;
    private static UserData user;
    private static UpdateUser updateUser;

    @Test
    @Feature("Authorization")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Успешная регистрация пользователя и получение его данных")
    void successfulRegisterAndGetUser(){
        step("Регистрация пользователя", () ->
                userId =
                        given()
                                .spec(request)
                                .body("{\"email\":\"" + userEmail + "\",\"password\": \"pistol\"}")
                                .when()
                                .post("/register")
                                .then()
                                .log().body()
                                .spec(responseSpec)
                                .body(matchesJsonSchemaInClasspath("schemas/UserRegistrationSchema.json"))
                                .extract()
                                .path("id")
        );

        step("Получение данных по зарегистрированному пользователю", () ->
                user = given()
                        .spec(request)
                        .when()
                        .get("/users/" + userId)
                        .then()
                        .log().body()
                        .spec(responseSpec)
                        .body(matchesJsonSchemaInClasspath("schemas/GetUserSchema.json"))
                        .extract().as(UserData.class)
        );

        step("Проверка данных по зарегистрированному пользователю", () ->
                assertAll(
                        () -> assertThat(user.getUser().getId()).isEqualTo(userId),
                        () -> assertThat(user.getUser().getEmail()).isEqualTo(userEmail),
                        () -> assertThat(user.getUser().getFirstName()).isEqualTo(firstName),
                        () -> assertThat(user.getUser().getLastName()).isEqualTo(lastName)
                )
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
                                .spec(request)
                                .when()
                                .get("/users")
                                .then()
                                .log().body()
                                .spec(responseSpec)
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
                updateUser = given()
                        .spec(request)
                        .body("{\"name\": \"" + userName + "\", \"job\":\"" + userJob + "\"}")
                        .when()
                        .post("/users/2")
                        .then()
                        .log().body()
                        .statusCode(201)
                        .body(matchesJsonSchemaInClasspath("schemas/UpdateUserSchema.json"))
                        .extract().as(UpdateUser.class)
        );

        step("Проверка обновленных данных пользователя", () ->
                assertAll(
                        () -> assertThat(updateUser.getName()).isEqualTo(userName),
                        () -> assertThat(updateUser.getJob()).isEqualTo(userJob)
                )
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
                                .spec(request)
                                .when()
                                .get("/users/" + userId)
                                .then()
                                .log().body()
                                .spec(responseSpec)
                                .body(matchesJsonSchemaInClasspath("schemas/GetUserSchema.json"))
                                .extract().as(UserData.class)
        );

        step("Получение списка пользователей и проверка наличия пользователя в списке", () ->
                        given()
                                .spec(request)
                                .when()
                                .get("/users")
                                .then()
                                .log().body()
                                .spec(responseSpec)
                                .body(matchesJsonSchemaInClasspath("schemas/ListUsersSchema.json"))
                                .body("data.findAll{it.email =~/.*?@reqres.in/}.email.flatten()",
                                        hasItem(user.getUser().getEmail()))
                                .body("data.findAll{it}.id.flatten()", hasItem(user.getUser().getId()))
                                .body("data.findAll{it}.first_name.flatten()", hasItem(user.getUser().getFirstName()))
                                .body("data.findAll{it}.last_name.flatten()", hasItem(user.getUser().getLastName()))
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
                        .spec(request)
                        .body("{\"email\": \"" + randomUserEmail + "\", \"password\": \"" + randomUserPassword + "\"}")
                        .when()
                        .post("/register")
                        .then()
                        .log().body()
                        .statusCode(400)
                        .body("error", is("Note: Only defined users succeed registration"))
        );
    }
}
