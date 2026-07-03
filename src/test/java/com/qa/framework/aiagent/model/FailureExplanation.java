package com.qa.framework.aiagent.model;

public record FailureExplanation(
        String stableId,
        String suite,
        String category,
        String confidence,
        String rootCause,
        String recommendedFix
) {

    public static FailureExplanation fallback(FailureContext context, String rawText) {
        return new FailureExplanation(
                context.stableId(),
                context.suite().name(),
                "UNKNOWN",
                "low",
                "Claude's response could not be parsed as structured JSON.",
                rawText == null || rawText.isBlank() ? "No recommendation available." : rawText
        );
    }
}
