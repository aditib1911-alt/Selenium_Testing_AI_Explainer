package com.qa.framework.aiagent.model;

public record FailureContext(
        String stableId,
        Suite suite,
        String testClassName,
        String testMethodName,
        String message,
        String stackTrace,
        String sourceSnippet,
        byte[] screenshot,
        String domSnapshot,
        String requestResponse
) {
    public enum Suite { UI, API }
}
