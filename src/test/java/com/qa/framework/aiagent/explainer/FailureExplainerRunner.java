package com.qa.framework.aiagent.explainer;

import com.qa.framework.aiagent.AiAgentConfig;
import com.qa.framework.aiagent.model.AllureTestResult;
import com.qa.framework.aiagent.model.FailureContext;
import com.qa.framework.aiagent.model.FailureExplanation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CI entry point, run via exec-maven-plugin after `mvn test`. Read-only: scans
 * allure-results for failures, asks Claude for a root cause + recommended fix per
 * failure, and writes an aggregated Markdown report for a PR comment step to post.
 * Never patches source, never opens a branch or PR.
 */
public final class FailureExplainerRunner {

    public static void main(String[] args) throws Exception {
        FailureCollector collector = new FailureCollector();
        List<AllureTestResult> failures = collector.collectFailures();

        if (failures.isEmpty()) {
            System.out.println("No failed tests found -- nothing to explain.");
            return;
        }

        FailureContextExtractor extractor = new FailureContextExtractor(Path.of(AiAgentConfig.allureResultsDir()));
        ClaudeFailureExplainer explainer = new ClaudeFailureExplainer();
        List<FailureExplanation> explanations = new ArrayList<>();

        for (AllureTestResult failure : failures) {
            FailureContext context = extractor.extract(failure);
            try {
                explanations.add(explainer.explain(context));
            } catch (Exception e) {
                System.err.println("Failed to get explanation for " + context.stableId() + ": " + e.getMessage());
            }
        }

        FailureExplanationReport report = new FailureExplanationReport();
        String rendered = report.render(explanations);
        Path outputPath = Path.of(AiAgentConfig.reportOutputPath());
        report.writeToFile(rendered, outputPath);
        System.out.println("Wrote failure explanations to " + outputPath);
    }
}
