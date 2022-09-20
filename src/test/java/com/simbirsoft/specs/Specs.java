package com.simbirsoft.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static com.simbirsoft.filters.CustomLogFilter.customLogFilter;
import static io.restassured.RestAssured.with;

public class Specs {

    public static RequestSpecification request = with()
            .baseUri("https://reqres.in")
            .basePath("/api")
            .log().uri()
            .log().body()
            .contentType(ContentType.JSON)
            .filter(customLogFilter().withCustomTemplates());

    public static ResponseSpecification responseSpec = new ResponseSpecBuilder()
            .expectStatusCode(200)
            .build();
}
