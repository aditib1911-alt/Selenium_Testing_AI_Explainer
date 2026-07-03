package com.qa.framework.aiagent.util;

import com.qa.framework.aiagent.model.FailureContext;

public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static String systemPrompt() {
        return """
                You are a read-only test-failure triage assistant for a Selenium/TestNG/RestAssured Java test suite.
                For each failure you are given, respond with ONLY a JSON object (no prose, no code fences) with the keys:
                "category" (one of the categories listed for this failure's suite), "confidence" ("high"|"medium"|"low"),
                "rootCause" (1-3 sentences explaining what most likely happened), and "recommendedFix" (a concrete,
                specific fix a human could apply -- e.g. the exact corrected CSS/XPath selector, or the exact
                assertion/code change -- never say "investigate further" without also giving your best concrete guess).
                You must NEVER claim to have applied a fix, opened a pull request, or changed any file -- you are
                strictly read-only and your output is advisory text for a human reviewer.
                """;
    }

    public static String userPrompt(FailureContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Test ID: ").append(context.stableId()).append('\n');
        sb.append("Suite: ").append(context.suite()).append('\n');
        sb.append("Test class: ").append(context.testClassName()).append('\n');
        sb.append("Test method: ").append(context.testMethodName()).append('\n');
        sb.append("Failure message: ").append(orNone(context.message())).append('\n');
        sb.append("Categories to choose from: ").append(categoriesFor(context)).append('\n');
        sb.append("\nStack trace:\n").append(orNone(context.stackTrace())).append('\n');

        if (context.suite() == FailureContext.Suite.UI) {
            sb.append("\nPage object source:\n").append(orNone(context.sourceSnippet())).append('\n');
            sb.append("\nDOM snapshot at time of failure:\n").append(orNone(context.domSnapshot())).append('\n');
            if (context.screenshot() != null) {
                sb.append("\nA screenshot at the time of failure is attached as an image.\n");
            }
        } else {
            sb.append("\nTest/API client source:\n").append(orNone(context.sourceSnippet())).append('\n');
            sb.append("\nCaptured request/response:\n").append(orNone(context.requestResponse())).append('\n');
            sb.append("\nNote: reqres.in is a mock API -- writes are echoed back but never persisted, and its API ")
                    .append("key enforcement has been observed to be non-deterministic. Consider ")
                    .append("MOCK_API_NONDETERMINISM when the evidence points that way.\n");
        }
        return sb.toString();
    }

    private static String categoriesFor(FailureContext context) {
        return context.suite() == FailureContext.Suite.UI
                ? "LOCATOR_DRIFT, APP_BUG, TIMING_FLAKINESS, ENVIRONMENT"
                : "APP_BUG, TEST_BUG, ENVIRONMENT_OR_NETWORK, MOCK_API_NONDETERMINISM";
    }

    private static String orNone(String value) {
        return (value == null || value.isBlank()) ? "(none captured)" : value;
    }
}
