package com.qa.framework.aiagent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AllureTestResult(
        String uuid,
        String fullName,
        String name,
        String status,
        StatusDetails statusDetails,
        List<Label> labels,
        List<Attachment> attachments
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusDetails(String message, String trace) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Label(String name, String value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Attachment(String name, String source, String type) {
    }

    public String testClass() {
        if (labels == null) {
            return null;
        }
        return labels.stream()
                .filter(label -> "testClass".equals(label.name()))
                .map(Label::value)
                .findFirst()
                .orElse(null);
    }

    public boolean isFailed() {
        return "failed".equals(status) || "broken".equals(status);
    }
}
