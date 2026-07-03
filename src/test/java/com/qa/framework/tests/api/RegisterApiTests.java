package com.qa.framework.tests.api;

import com.qa.framework.api.ApiClient;
import com.qa.framework.api.models.RegisterRequest;
import com.qa.framework.base.BaseApiTest;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

public class RegisterApiTests extends BaseApiTest {

    private ApiClient apiClient;

    @BeforeClass(alwaysRun = true, dependsOnMethods = "baseSetUp")
    public void setUpClient() {
        apiClient = new ApiClient(requestSpec);
    }

    @Test(description = "API-09")
    public void registerWithValidCredentialsSucceeds() {
        RegisterRequest request = new RegisterRequest("eve.holt@reqres.in", "pistol");
        Response response = apiClient.register(request);

        response.then().statusCode(200).body(matchesJsonSchemaInClasspath("schemas/register-schema.json"));
        assertThat(response.jsonPath().getString("token")).isNotBlank();
    }

    @Test(description = "API-10")
    public void registerWithMissingPasswordReturns400() {
        RegisterRequest request = new RegisterRequest("eve.holt@reqres.in", null);
        Response response = apiClient.register(request);

        response.then().statusCode(400);
        assertThat(response.jsonPath().getString("error")).containsIgnoringCase("password");
    }

    @Test(description = "API-11")
    public void registerWithMissingEmailReturns400() {
        RegisterRequest request = new RegisterRequest(null, "pistol");
        Response response = apiClient.register(request);

        response.then().statusCode(400);
        assertThat(response.jsonPath().getString("error")).isNotBlank();
    }

    @Test(description = "API-12")
    public void loginWithValidCredentialsSucceeds() {
        RegisterRequest request = new RegisterRequest("eve.holt@reqres.in", "cityslicka");
        Response response = apiClient.login(request);

        response.then().statusCode(200);
        assertThat(response.jsonPath().getString("token")).isNotBlank();
    }

    @Test(description = "API-13")
    public void loginWithMissingPasswordReturns400() {
        RegisterRequest request = new RegisterRequest("eve.holt@reqres.in", null);
        Response response = apiClient.login(request);

        response.then().statusCode(400);
        assertThat(response.jsonPath().getString("error")).containsIgnoringCase("password");
    }
}
