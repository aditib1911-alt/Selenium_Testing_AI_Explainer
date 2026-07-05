package com.qa.framework.aiagent.explainer;

import com.qa.framework.aiagent.AiAgentConfig;
import com.qa.framework.aiagent.model.AllureTestResult;
import com.qa.framework.aiagent.model.FailureContext;
import com.qa.framework.aiagent.model.FailureExplanation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Local CLI entry point, run manually via exec-maven-plugin after `mvn test` (see README).
 * Read-only: scans allure-results for failures and, for each one, asks Claude for a root
 * cause + recommended fix, printing that explanation to the console as soon as it's ready.
 * All explanations are also written to a single Markdown file for later reference. Never
 * patches source, commits, or opens a branch/PR -- purely local, advisory output.
 */
public final class FailureExplainerRunner {

    public static void main(String[] args) throws Exception {
        FailureCollector collector = new FailureCollector();
        List<AllureTestResult> failures = collector.collectFailures();

        if (failures.isEmpty()) {
            System.out.println("No failed tests found -- nothing to explain.");
            return;
        }

        System.out.println(failures.size() + " failed test(s) found. Asking Claude to explain each one...\n");

        FailureContextExtractor extractor = new FailureContextExtractor(Path.of(AiAgentConfig.allureResultsDir()));
        ClaudeFailureExplainer explainer = new ClaudeFailureExplainer();
        FailureExplanationReport report = new FailureExplanationReport();
        List<FailureExplanation> explanations = new ArrayList<>();

        int index = 0;
        for (AllureTestResult failure : failures) {
            index++;
            FailureContext context = extractor.extract(failure);
            System.out.println("[" + index + "/" + failures.size() + "] Analyzing " + context.stableId() + "...");
            try {
                FailureExplanation explanation = explainer.explain(context);
                explanations.add(explanation);
                System.out.println(report.renderOne(explanation));
            } catch (Exception e) {
                System.err.println("Failed to get explanation for " + context.stableId() + ": " + e.getMessage());
            }
        }

        Path outputPath = Path.of(AiAgentConfig.reportOutputPath());
        report.writeToFile(report.render(explanations), outputPath);
        System.out.println("Wrote " + explanations.size() + " explanation(s) to " + outputPath);
    }
}
