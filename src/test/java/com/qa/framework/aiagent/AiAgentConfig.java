package com.qa.framework.aiagent;

import com.qa.framework.utils.ConfigReader;

/**
 * Config for the AI failure explainer. Keys resolve through ConfigReader's existing
 * env-var-overrides-properties convention, e.g. anthropic.api.key -> ANTHROPIC_API_KEY.
 */
public final class AiAgentConfig {

    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final String DEFAULT_RESULTS_DIR = "allure-results";
    private static final String DEFAULT_REPORT_PATH = "target/aiagent/failure-explanations.md";

    private AiAgentConfig() {
    }

    public static String apiKey() {
        return ConfigReader.getRequired("anthropic.api.key",
                "Set env var ANTHROPIC_API_KEY to enable the failure explainer.");
    }

    public static String model() {
        String configured = ConfigReader.get("anthropic.model");
        return isBlank(configured) ? DEFAULT_MODEL : configured;
    }

    public static String allureResultsDir() {
        String configured = ConfigReader.get("allure.results.directory");
        return isBlank(configured) ? DEFAULT_RESULTS_DIR : configured;
    }

    public static String reportOutputPath() {
        String configured = ConfigReader.get("aiagent.report.path");
        return isBlank(configured) ? DEFAULT_REPORT_PATH : configured;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
