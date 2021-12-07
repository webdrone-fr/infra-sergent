package com.webdrone.sergent;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class AgentResourceTest {

    @Test
    public void testSergentEndpoint() {
        given()
          .when().get("/sergent?command=list")
          .then()
             .statusCode(200)
             .body(is("{\"commands\":[\"list\",\"dockerpull\",\"gitpull\"]}"));
    }

}