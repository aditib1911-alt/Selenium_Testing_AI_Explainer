package com.qa.framework.aiagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.framework.aiagent.model.AllureTestResult;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AllureResultReader {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<AllureTestResult> readAll(Path resultsDir) throws IOException {
        List<AllureTestResult> results = new ArrayList<>();
        if (!Files.isDirectory(resultsDir)) {
            return results;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(resultsDir, "*-result.json")) {
            for (Path path : stream) {
                results.add(mapper.readValue(path.toFile(), AllureTestResult.class));
            }
        }
        return results;
    }
}
