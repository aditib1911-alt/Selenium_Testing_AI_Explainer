package com.qa.framework.aiagent.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceFileLocator {

    private static final Path SOURCE_ROOT = Path.of("src", "test", "java");

    private SourceFileLocator() {
    }

    public static String readSource(String fullyQualifiedClassName) {
        if (fullyQualifiedClassName == null) {
            return null;
        }
        Path path = SOURCE_ROOT.resolve(fullyQualifiedClassName.replace('.', '/') + ".java");
        try {
            return Files.exists(path) ? Files.readString(path) : null;
        } catch (IOException e) {
            return null;
        }
    }
}
