package com.example.fileindexer.model;

import java.nio.file.Path;

/** A selectable root (a drive letter on Windows, "/" on Linux/macOS). */
public record Drive(Path root, String label) {

    public String id() {
        return root.toString();
    }
}
