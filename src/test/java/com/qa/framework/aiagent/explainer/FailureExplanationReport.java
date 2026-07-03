package com.qa.framework.aiagent.explainer;

import com.qa.framework.aiagent.model.FailureExplanation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FailureExplanationReport {

    public String render(List<FailureExplanation> explanations) {
        if (explanations.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## AI Failure Explainer\n\n");
        sb.append(explanations.size())
                .append(" test failure(s) analyzed. This is read-only analysis -- no code was changed.\n\n");
        for (FailureExplanation explanation : explanations) {
            sb.append("### ").append(explanation.stableId()).append(" (").append(explanation.suite()).append(")\n");
            sb.append("- **Category:** ").append(explanation.category()).append('\n');
            sb.append("- **Confidence:** ").append(explanation.confidence()).append('\n');
            sb.append("- **Root cause:** ").append(explanation.rootCause()).append('\n');
            sb.append("- **Recommended fix:** ").append(explanation.recommendedFix()).append('\n');
            sb.append('\n');
        }
        return sb.toString();
    }

    public void writeToFile(String content, Path outputPath) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.writeString(outputPath, content);
    }
}
