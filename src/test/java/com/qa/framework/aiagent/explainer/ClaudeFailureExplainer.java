package com.qa.framework.aiagent.explainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qa.framework.aiagent.AiAgentConfig;
import com.qa.framework.aiagent.model.FailureContext;
import com.qa.framework.aiagent.model.FailureExplanation;
import com.qa.framework.aiagent.util.PromptTemplates;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * Calls the Claude Messages API directly over HTTPS (no SDK dependency needed --
 * Jackson is already on the classpath via allure/rest-assured). Read-only: this class
 * never writes to source files or opens branches/PRs.
 */
public final class ClaudeFailureExplainer {

    private static final URI ENDPOINT = URI.create("https://api.anthropic.com/v1/messages");
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1024;

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClaudeFailureExplainer() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    public FailureExplanation explain(FailureContext context) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .header("x-api-key", AiAgentConfig.apiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(buildRequestBody(context))))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Claude API call failed: HTTP " + response.statusCode() + " " + response.body());
        }
        return parseExplanation(context, response.body());
    }

    private ObjectNode buildRequestBody(FailureContext context) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", AiAgentConfig.model());
        root.put("max_tokens", MAX_TOKENS);
        root.put("system", PromptTemplates.systemPrompt());

        ArrayNode contentBlocks = mapper.createArrayNode();

        ObjectNode textBlock = mapper.createObjectNode();
        textBlock.put("type", "text");
        textBlock.put("text", PromptTemplates.userPrompt(context));
        contentBlocks.add(textBlock);

        if (context.screenshot() != null) {
            ObjectNode imageBlock = mapper.createObjectNode();
            imageBlock.put("type", "image");
            ObjectNode source = mapper.createObjectNode();
            source.put("type", "base64");
            source.put("media_type", "image/png");
            source.put("data", Base64.getEncoder().encodeToString(context.screenshot()));
            imageBlock.set("source", source);
            contentBlocks.add(imageBlock);
        }

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.set("content", contentBlocks);

        ArrayNode messages = mapper.createArrayNode();
        messages.add(userMessage);
        root.set("messages", messages);
        return root;
    }

    private FailureExplanation parseExplanation(FailureContext context, String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        StringBuilder text = new StringBuilder();
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }

        JsonNode parsed;
        try {
            parsed = mapper.readTree(extractJsonObject(text.toString()));
        } catch (IOException malformed) {
            return FailureExplanation.fallback(context, text.toString());
        }

        return new FailureExplanation(
                context.stableId(),
                context.suite().name(),
                textOrDefault(parsed, "category", "UNKNOWN"),
                textOrDefault(parsed, "confidence", "unknown"),
                textOrDefault(parsed, "rootCause", ""),
                textOrDefault(parsed, "recommendedFix", "")
        );
    }

    private static String textOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        return (value == null || value.isMissingNode() || value.isNull()) ? defaultValue : value.asText();
    }

    private String extractJsonObject(String raw) {
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        return (start >= 0 && end > start) ? trimmed.substring(start, end + 1) : trimmed;
    }
}
