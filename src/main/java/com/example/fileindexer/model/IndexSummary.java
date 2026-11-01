package com.example.fileindexer.model;

public record IndexSummary(long added, long updated, long removed, long unchanged, long skipped, long failed) {

    public IndexSummary plus(IndexSummary o) {
        return new IndexSummary(added + o.added, updated + o.updated, removed + o.removed,
                unchanged + o.unchanged, skipped + o.skipped, failed + o.failed);
    }

    public static IndexSummary empty() {
        return new IndexSummary(0, 0, 0, 0, 0, 0);
    }
}
