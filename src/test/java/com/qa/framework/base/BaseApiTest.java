package com.qa.framework.base;

import com.qa.framework.utils.ConfigReader;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;

public abstract class BaseApiTest {

    protected RequestSpecification requestSpec;

    @BeforeClass(alwaysRun = true)
    public void baseSetUp() {
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(ConfigReader.get("api.base.url"))
                .setContentType(ContentType.JSON)
                .addHeader("x-api-key", ConfigReader.getReqresApiKey())
                .addFilter(new AllureRestAssured())
                .build();
    }
}
