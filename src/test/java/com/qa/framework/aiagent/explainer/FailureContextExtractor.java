package com.qa.framework.aiagent.explainer;

import com.qa.framework.aiagent.model.AllureTestResult;
import com.qa.framework.aiagent.model.FailureContext;
import com.qa.framework.aiagent.util.SourceFileLocator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a FailureContext from an Allure result: resolves the relevant source snippet
 * (page object for UI, test/API client for API), and pulls in whatever evidence
 * TestListener/AllureRestAssured already attached (screenshot, DOM snapshot, request/response).
 */
public final class FailureContextExtractor {

    private static final Pattern PAGE_OBJECT_FRAME = Pattern.compile("at (com\\.qa\\.framework\\.pages\\.\\w+)\\.");
    private static final int MAX_SNIPPET_CHARS = 6000;
    private static final String UI_TEST_PACKAGE_PREFIX = "com.qa.framework.tests.ui.";

    private final Path resultsDir;

    public FailureContextExtractor(Path resultsDir) {
        this.resultsDir = resultsDir;
    }

    public FailureContext extract(AllureTestResult result) {
        String testClass = result.testClass();
        boolean isUi = testClass != null && testClass.startsWith(UI_TEST_PACKAGE_PREFIX);
        FailureContext.Suite suite = isUi ? FailureContext.Suite.UI : FailureContext.Suite.API;

        String trace = statusValue(result, true);
        String message = statusValue(result, false);

        String sourceSnippet = isUi ? resolvePageObjectSource(trace) : SourceFileLocator.readSource(testClass);
        String domSnapshot = isUi ? readTextAttachmentBySuffix(result, "-dom") : null;
        byte[] screenshot = isUi ? readBinaryAttachmentBySuffix(result, "-failure") : null;
        String requestResponse = isUi ? null : buildRequestResponse(result);

        return new FailureContext(
                result.name(),
                suite,
                testClass,
                lastSegment(result.fullName()),
                truncate(message),
                truncate(trace),
                truncate(sourceSnippet),
                screenshot,
                truncate(domSnapshot),
                truncate(requestResponse)
        );
    }

    private String statusValue(AllureTestResult result, boolean trace) {
        if (result.statusDetails() == null) {
            return "";
        }
        String value = trace ? result.statusDetails().trace() : result.statusDetails().message();
        return value == null ? "" : value;
    }

    private String resolvePageObjectSource(String trace) {
        Matcher matcher = PAGE_OBJECT_FRAME.matcher(trace);
        return matcher.find() ? SourceFileLocator.readSource(matcher.group(1)) : null;
    }

    private String buildRequestResponse(AllureTestResult result) {
        List<AllureTestResult.Attachment> attachments = result.attachments();
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        attachments.stream()
                .filter(a -> "Request".equals(a.name()))
                .findFirst()
                .ifPresent(a -> sb.append("Request:\n").append(readAttachmentText(a.source())).append("\n\n"));
        attachments.stream()
                .filter(a -> a.name() != null && a.name().startsWith("HTTP/"))
                .findFirst()
                .ifPresent(a -> sb.append("Response (").append(a.name()).append("):\n")
                        .append(readAttachmentText(a.source())));
        return sb.isEmpty() ? null : sb.toString();
    }

    private String readTextAttachmentBySuffix(AllureTestResult result, String suffix) {
        List<AllureTestResult.Attachment> attachments = result.attachments();
        if (attachments == null) {
            return null;
        }
        return attachments.stream()
                .filter(a -> a.name() != null && a.name().endsWith(suffix))
                .findFirst()
                .map(a -> readAttachmentText(a.source()))
                .orElse(null);
    }

    private byte[] readBinaryAttachmentBySuffix(AllureTestResult result, String suffix) {
        List<AllureTestResult.Attachment> attachments = result.attachments();
        if (attachments == null) {
            return null;
        }
        return attachments.stream()
                .filter(a -> a.name() != null && a.name().endsWith(suffix))
                .findFirst()
                .map(a -> readAttachmentBytes(a.source()))
                .orElse(null);
    }

    private String readAttachmentText(String sourceFileName) {
        try {
            return Files.readString(resultsDir.resolve(sourceFileName));
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] readAttachmentBytes(String sourceFileName) {
        try {
            return Files.readAllBytes(resultsDir.resolve(sourceFileName));
        } catch (IOException e) {
            return null;
        }
    }

    private static String lastSegment(String fullName) {
        if (fullName == null) {
            return null;
        }
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_SNIPPET_CHARS) {
            return value;
        }
        return value.substring(0, MAX_SNIPPET_CHARS) + "\n... (truncated)";
    }
}
