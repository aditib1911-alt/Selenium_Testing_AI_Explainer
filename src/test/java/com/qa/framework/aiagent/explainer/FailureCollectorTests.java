package com.qa.framework.aiagent.explainer;

import com.qa.framework.aiagent.AllureResultReader;
import com.qa.framework.aiagent.model.AllureTestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FailureCollectorTests {

    private Path resultsDir;

    @BeforeMethod
    public void setUp() throws IOException {
        resultsDir = Files.createTempDirectory("aiagent-collector-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        try (var files = Files.walk(resultsDir)) {
            files.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }

    @Test(description = "AIAGENT-04")
    public void onlyFailedAndBrokenResultsAreCollected() throws IOException {
        writeResult("passed-result.json", "LOGIN-01", "passed");
        writeResult("failed-result.json", "LOGIN-02", "failed");
        writeResult("broken-result.json", "API-05", "broken");
        writeResult("skipped-result.json", "API-06", "skipped");

        FailureCollector collector = new FailureCollector(new AllureResultReader(), resultsDir);
        List<AllureTestResult> failures = collector.collectFailures();

        assertThat(failures).extracting(AllureTestResult::name)
                .containsExactlyInAnyOrder("LOGIN-02", "API-05");
    }

    @Test(description = "AIAGENT-05")
    public void missingResultsDirectoryYieldsNoFailures() throws IOException {
        FailureCollector collector = new FailureCollector(new AllureResultReader(), resultsDir.resolve("does-not-exist"));
        assertThat(collector.collectFailures()).isEmpty();
    }

    private void writeResult(String fileName, String stableId, String status) throws IOException {
        String json = """
                {
                  "uuid": "%s",
                  "fullName": "com.qa.framework.tests.ui.LoginTests.example",
                  "name": "%s",
                  "status": "%s",
                  "statusDetails": {"message": "m", "trace": "t"},
                  "labels": [{"name": "testClass", "value": "com.qa.framework.tests.ui.LoginTests"}],
                  "attachments": []
                }
                """.formatted(fileName, stableId, status);
        Files.writeString(resultsDir.resolve(fileName), json);
    }
}
