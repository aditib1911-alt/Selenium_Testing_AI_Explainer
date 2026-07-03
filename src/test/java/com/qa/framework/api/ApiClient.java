package com.qa.framework.api;

import com.qa.framework.api.models.RegisterRequest;
import com.qa.framework.api.models.UserRequest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Thin wrapper around a shared RequestSpecification so base URI, content-type,
 * and the x-api-key header are configured in exactly one place (BaseApiTest).
 */
public class ApiClient {

    private final RequestSpecification requestSpec;

    public ApiClient(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    public Response getUser(int id) {
        return given().spec(requestSpec).pathParam("id", id).when().get(ApiEndpoints.USER_BY_ID);
    }

    public Response getUsers(int page) {
        return given().spec(requestSpec).queryParam("page", page).when().get(ApiEndpoints.USERS);
    }

    public Response createUser(UserRequest user) {
        return given().spec(requestSpec).body(user).when().post(ApiEndpoints.USERS);
    }

    public Response updateUser(int id, UserRequest user) {
        return given().spec(requestSpec).pathParam("id", id).body(user).when().put(ApiEndpoints.USER_BY_ID);
    }

    public Response patchUser(int id, UserRequest user) {
        return given().spec(requestSpec).pathParam("id", id).body(user).when().patch(ApiEndpoints.USER_BY_ID);
    }

    public Response deleteUser(int id) {
        return given().spec(requestSpec).pathParam("id", id).when().delete(ApiEndpoints.USER_BY_ID);
    }

    public Response register(RegisterRequest registerRequest) {
        return given().spec(requestSpec).body(registerRequest).when().post(ApiEndpoints.REGISTER);
    }

    public Response login(RegisterRequest loginRequest) {
        return given().spec(requestSpec).body(loginRequest).when().post(ApiEndpoints.LOGIN);
    }
}
