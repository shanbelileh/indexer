package com.example.fileindexer.model;

public enum SearchType {
    /** Lucene query syntax: words, "phrases", wildcards, AND/OR/NOT. */
    REGULAR,
    /** Lucene regular expression matched against indexed (lower-cased) terms. */
    REGEX
}
