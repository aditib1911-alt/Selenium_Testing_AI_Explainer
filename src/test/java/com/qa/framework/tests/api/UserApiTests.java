package com.qa.framework.tests.api;

import com.qa.framework.api.ApiClient;
import com.qa.framework.api.models.UserRequest;
import com.qa.framework.base.BaseApiTest;
import com.qa.framework.constants.FrameworkConstants;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * reqres.in is a mock API: POST/PUT/PATCH/DELETE responses are echoed back but never
 * actually persisted server-side. Every assertion here checks the response of the
 * SAME call that produced it -- never a follow-up GET, which would hit the static
 * mock dataset and produce a false failure.
 *
 * No missing/invalid-API-key negative test: verified live that reqres.in's own key
 * enforcement is non-deterministic (401 on one run, 200 with no key at all a minute
 * later, no code change on our side) -- asserting a specific status code there would
 * be flaky by construction, driven entirely by the third-party mock, not our code.
 */
public class UserApiTests extends BaseApiTest {

    private ApiClient apiClient;

    @BeforeClass(alwaysRun = true, dependsOnMethods = "baseSetUp")
    public void setUpClient() {
        apiClient = new ApiClient(requestSpec);
    }

    @Test(description = "API-01")
    public void getSingleUserReturnsSchemaValidBody() {
        Response response = apiClient.getUser(2);

        response.then().statusCode(200).body(matchesJsonSchemaInClasspath("schemas/user-single-schema.json"));
        assertThat(response.jsonPath().getInt("data.id")).isEqualTo(2);
    }

    @Test(description = "API-02")
    public void getUserNotFoundReturns404() {
        apiClient.getUser(23).then().statusCode(404);
    }

    @Test(description = "API-03")
    public void getUserListPageReturnsSchemaValidBody() {
        Response response = apiClient.getUsers(2);

        response.then().statusCode(200).body(matchesJsonSchemaInClasspath("schemas/user-list-schema.json"));
        assertThat(response.jsonPath().getInt("page")).isEqualTo(2);
        assertThat(response.jsonPath().getList("data")).isNotEmpty();
    }

    @Test(description = "API-04")
    public void getUserListOutOfRangePageReturnsEmptyData() {
        Response response = apiClient.getUsers(999);

        response.then().statusCode(200);
        assertThat(response.jsonPath().getList("data")).isEmpty();
    }

    @Test(description = "API-05")
    public void createUserReturns201WithEchoedFields() {
        UserRequest newUser = new UserRequest("morpheus", "leader");
        Response response = apiClient.createUser(newUser);

        response.then().statusCode(201);
        assertThat(response.jsonPath().getString("name")).isEqualTo("morpheus");
        assertThat(response.jsonPath().getString("job")).isEqualTo("leader");
        assertThat(response.jsonPath().getString("id")).isNotBlank();
        assertThat(response.jsonPath().getString("createdAt")).isNotBlank();
    }

    @Test(description = "API-06")
    public void updateUserReturns200WithEchoedFields() {
        UserRequest updatedUser = new UserRequest("morpheus", "zion resident");
        Response response = apiClient.updateUser(2, updatedUser);

        response.then().statusCode(200);
        assertThat(response.jsonPath().getString("job")).isEqualTo("zion resident");
        assertThat(response.jsonPath().getString("updatedAt")).isNotBlank();
    }

    @Test(description = "API-07")
    public void patchUserReturns200WithEchoedField() {
        UserRequest patch = new UserRequest(null, "delegator");
        Response response = apiClient.patchUser(2, patch);

        response.then().statusCode(200);
        assertThat(response.jsonPath().getString("job")).isEqualTo("delegator");
        assertThat(response.jsonPath().getString("updatedAt")).isNotBlank();
    }

    @Test(description = "API-08")
    public void deleteUserReturns204() {
        apiClient.deleteUser(2).then().statusCode(204).body(org.hamcrest.Matchers.blankOrNullString());
    }

    @Test(description = "API-14")
    public void getSingleUserRespondsWithinThreshold() {
        Response response = apiClient.getUser(2);
        assertThat(response.getTime()).isLessThan(FrameworkConstants.API_RESPONSE_TIME_THRESHOLD_MS);
    }
}
