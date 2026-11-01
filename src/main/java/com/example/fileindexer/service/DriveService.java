package com.example.fileindexer.service;

import com.example.fileindexer.model.Drive;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DriveService {

    public List<Drive> listDrives() {
        List<Drive> drives = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            if (!Files.isReadable(root)) {
                continue; // e.g. empty card reader on Windows
            }
            drives.add(new Drive(root, describe(root)));
        }
        return drives;
    }

    private static String describe(Path root) {
        try {
            FileStore store = Files.getFileStore(root);
            return "%s  (%s, %s total, %s free)".formatted(
                    root, store.type(), gb(store.getTotalSpace()), gb(store.getUsableSpace()));
        } catch (IOException e) {
            return root.toString();
        }
    }

    private static String gb(long bytes) {
        return "%.1f GB".formatted(bytes / (1024.0 * 1024 * 1024));
    }
}
