package com.qa.framework.aiagent.explainer;

import com.qa.framework.aiagent.AiAgentConfig;
import com.qa.framework.aiagent.AllureResultReader;
import com.qa.framework.aiagent.model.AllureTestResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class FailureCollector {

    private final AllureResultReader reader;
    private final Path resultsDir;

    public FailureCollector() {
        this(new AllureResultReader(), Path.of(AiAgentConfig.allureResultsDir()));
    }

    public FailureCollector(AllureResultReader reader, Path resultsDir) {
        this.reader = reader;
        this.resultsDir = resultsDir;
    }

    public List<AllureTestResult> collectFailures() throws IOException {
        return reader.readAll(resultsDir).stream()
                .filter(AllureTestResult::isFailed)
                .toList();
    }
}
