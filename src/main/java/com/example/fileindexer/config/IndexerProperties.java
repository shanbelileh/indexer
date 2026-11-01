package com.example.fileindexer.config;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "indexer")
public record IndexerProperties(
        List<String> extensions,
        List<String> excludedDirs,
        String luceneDir,
        long maxFileSizeMb,
        int maxResults) {

    public IndexerProperties {
        extensions = extensions == null ? List.of() : extensions;
        excludedDirs = excludedDirs == null ? List.of() : excludedDirs;
        luceneDir = (luceneDir == null || luceneDir.isBlank()) ? "./data/lucene" : luceneDir;
        maxFileSizeMb = maxFileSizeMb <= 0 ? 10 : maxFileSizeMb;
        maxResults = maxResults <= 0 ? 50 : maxResults;
    }

    /**
     * Lower-cased extensions without a leading dot.
     */
    public Set<String> extensionSet() {
        return extensions.stream()
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .map(e -> e.startsWith(".") ? e.substring(1) : e)
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> excludedDirSet() {
        return excludedDirs.stream()
                .map(d -> d.trim().toLowerCase(Locale.ROOT))
                .filter(d -> !d.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public long maxFileSizeBytes() {
        return maxFileSizeMb * 1024 * 1024;
    }
}
