package com.qa.framework.aiagent.explainer;

import com.qa.framework.aiagent.model.FailureExplanation;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FailureExplanationReportTests {

    @Test(description = "AIAGENT-06")
    public void emptyExplanationsProduceEmptyReport() {
        assertThat(new FailureExplanationReport().render(List.of())).isEmpty();
    }

    @Test(description = "AIAGENT-07")
    public void reportIncludesRootCauseAndRecommendedFixPerFailure() {
        FailureExplanation explanation = new FailureExplanation(
                "LOGIN-02", "UI", "LOCATOR_DRIFT", "high",
                "The data-test attribute on the login button changed.",
                "Update LoginPage.LOGIN_BUTTON to By.cssSelector(\"[data-test='login-submit']\")");

        String rendered = new FailureExplanationReport().render(List.of(explanation));

        assertThat(rendered)
                .contains("LOGIN-02")
                .contains("LOCATOR_DRIFT")
                .contains("high")
                .contains("data-test attribute on the login button changed")
                .contains("LOGIN_BUTTON")
                .contains("no code was changed");
    }
}
