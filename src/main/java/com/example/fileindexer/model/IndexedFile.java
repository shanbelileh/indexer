package com.example.fileindexer.model;

public record IndexedFile(String path, String drive, long size, long lastModified) {
}
