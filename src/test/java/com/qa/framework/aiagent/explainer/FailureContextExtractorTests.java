package com.qa.framework.aiagent.explainer;

import com.qa.framework.aiagent.model.AllureTestResult;
import com.qa.framework.aiagent.model.FailureContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FailureContextExtractorTests {

    private Path resultsDir;
    private FailureContextExtractor extractor;

    @BeforeMethod
    public void setUp() throws IOException {
        resultsDir = Files.createTempDirectory("aiagent-test");
        extractor = new FailureContextExtractor(resultsDir);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        try (var files = Files.walk(resultsDir)) {
            files.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    @Test(description = "AIAGENT-01")
    public void uiFailureResolvesPageObjectSourceDomAndScreenshot() throws IOException {
        Files.writeString(resultsDir.resolve("dom.html"), "<html><body>stale</body></html>");
        Files.write(resultsDir.resolve("shot.png"), new byte[]{1, 2, 3});

        AllureTestResult.StatusDetails details = new AllureTestResult.StatusDetails(
                "no such element",
                "org.openqa.selenium.NoSuchElementException: boom\n\tat com.qa.framework.pages.LoginPage.login(LoginPage.java:20)\n");
        AllureTestResult.Label testClassLabel = new AllureTestResult.Label(
                "testClass", "com.qa.framework.tests.ui.LoginTests");
        AllureTestResult.Attachment domAttachment = new AllureTestResult.Attachment(
                "loginFails-dom", "dom.html", "text/html");
        AllureTestResult.Attachment screenshotAttachment = new AllureTestResult.Attachment(
                "loginFails-failure", "shot.png", "image/png");

        AllureTestResult result = new AllureTestResult(
                "uuid-1", "com.qa.framework.tests.ui.LoginTests.loginFails", "LOGIN-01", "failed",
                details, List.of(testClassLabel), List.of(domAttachment, screenshotAttachment));

        FailureContext context = extractor.extract(result);

        assertThat(context.suite()).isEqualTo(FailureContext.Suite.UI);
        assertThat(context.stableId()).isEqualTo("LOGIN-01");
        assertThat(context.testMethodName()).isEqualTo("loginFails");
        assertThat(context.sourceSnippet()).contains("class LoginPage");
        assertThat(context.domSnapshot()).isEqualTo("<html><body>stale</body></html>");
        assertThat(context.screenshot()).containsExactly(1, 2, 3);
        assertThat(context.requestResponse()).isNull();
    }

    @Test(description = "AIAGENT-02")
    public void apiFailureResolvesTestSourceAndRequestResponse() throws IOException {
        Files.writeString(resultsDir.resolve("req.html"), "GET /api/users/2", StandardCharsets.UTF_8);
        Files.writeString(resultsDir.resolve("resp.html"), "{\"error\":\"not found\"}", StandardCharsets.UTF_8);

        AllureTestResult.StatusDetails details = new AllureTestResult.StatusDetails(
                "Expected status code <200> but was <404>.",
                "java.lang.AssertionError: Expected status code <200> but was <404>.\n\tat com.qa.framework.tests.api.UserApiTests.getUserById(UserApiTests.java:42)\n");
        AllureTestResult.Label testClassLabel = new AllureTestResult.Label(
                "testClass", "com.qa.framework.tests.api.UserApiTests");
        AllureTestResult.Attachment requestAttachment = new AllureTestResult.Attachment(
                "Request", "req.html", "text/html");
        AllureTestResult.Attachment responseAttachment = new AllureTestResult.Attachment(
                "HTTP/1.1 404 Not Found", "resp.html", "text/html");

        AllureTestResult result = new AllureTestResult(
                "uuid-2", "com.qa.framework.tests.api.UserApiTests.getUserById", "API-05", "failed",
                details, List.of(testClassLabel), List.of(requestAttachment, responseAttachment));

        FailureContext context = extractor.extract(result);

        assertThat(context.suite()).isEqualTo(FailureContext.Suite.API);
        assertThat(context.sourceSnippet()).contains("class UserApiTests");
        assertThat(context.requestResponse()).contains("GET /api/users/2").contains("not found");
        assertThat(context.domSnapshot()).isNull();
        assertThat(context.screenshot()).isNull();
    }

    @Test(description = "AIAGENT-03")
    public void missingAttachmentsResolveToNullWithoutThrowing() {
        AllureTestResult.StatusDetails details = new AllureTestResult.StatusDetails("boom", "boom");
        AllureTestResult.Label testClassLabel = new AllureTestResult.Label(
                "testClass", "com.qa.framework.tests.ui.DashboardTests");
        AllureTestResult result = new AllureTestResult(
                "uuid-3", "com.qa.framework.tests.ui.DashboardTests.addToCart", "DASH-01", "failed",
                details, List.of(testClassLabel), List.of());

        FailureContext context = extractor.extract(result);

        assertThat(context.domSnapshot()).isNull();
        assertThat(context.screenshot()).isNull();
    }
}
