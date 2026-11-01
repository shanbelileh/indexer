package com.example.fileindexer.service;

import com.example.fileindexer.config.IndexerProperties;
import com.example.fileindexer.lucene.LuceneStore;
import com.example.fileindexer.model.Drive;
import com.example.fileindexer.model.IndexSummary;
import com.example.fileindexer.model.IndexedFile;
import com.example.fileindexer.store.IndexedFileRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.stereotype.Service;

@Service
public class IndexingService {

    private static final int BINARY_SNIFF_BYTES = 4096;

    private final IndexerProperties props;
    private final LuceneStore lucene;
    private final IndexedFileRepository repository;

    public IndexingService(IndexerProperties props, LuceneStore lucene, IndexedFileRepository repository) {
        this.props = props;
        this.lucene = lucene;
        this.repository = repository;
    }

    /**
     * Indexes new or changed files of the given drives and removes entries of files that disappeared.
     *
     * @param progress receives one line per file that is indexed, updated, removed or failed
     */
    public IndexSummary index(List<Drive> drives, Consumer<String> progress) throws IOException {
        IndexSummary total = IndexSummary.empty();
        try (IndexWriter writer = lucene.openWriter()) {
            for (Drive drive : drives) {
                progress.accept(new AttributedString("== Scanning " + drive.root() + " ==",
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA)).toAnsi());
                IndexSummary summary = indexDrive(drive, writer, progress);
                writer.commit();
                progress.accept(new AttributedString("== Done " + drive.root() + ": " + describe(summary) + " ==",
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA)).toAnsi());
                total = total.plus(summary);
            }
        }
        return total;
    }

    public static String describe(IndexSummary s) {
        return "%d new, %d updated, %d removed, %d unchanged, %d skipped, %d failed"
                .formatted(s.added(), s.updated(), s.removed(), s.unchanged(), s.skipped(), s.failed());
    }

    private IndexSummary indexDrive(Drive drive, IndexWriter writer, Consumer<String> progress) throws IOException {
        Set<String> extensions = props.extensionSet();
        Set<String> excludedDirs = props.excludedDirSet();
        long maxBytes = props.maxFileSizeBytes();
        String driveId = drive.id();
        Map<String, IndexedFile> known = repository.findByDrive(driveId);
        Counters c = new Counters();

        Files.walkFileTree(drive.root(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path name = dir.getFileName();
                if (name != null && excludedDirs.contains(name.toString().toLowerCase(Locale.ROOT))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && hasWantedExtension(file, extensions)) {
                    handleFile(file, attrs, driveId, known, maxBytes, writer, progress, c);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE; // no permission, vanished file, ...
            }
        });

        // Whatever is still in the map was not seen on disk: it was deleted since the last run.
        for (String gone : known.keySet()) {
            writer.deleteDocuments(new Term(LuceneStore.F_PATH, gone));
            repository.delete(gone);
            c.removed++;
            progress.accept(new AttributedString("[REMOVED] " + gone,
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)).toAnsi());
        }
        return new IndexSummary(c.added, c.updated, c.removed, c.unchanged, c.skipped, c.failed);
    }

    private void handleFile(Path file, BasicFileAttributes attrs, String driveId, Map<String, IndexedFile> known,
                            long maxBytes, IndexWriter writer, Consumer<String> progress, Counters c) {
        String key = file.toString();
        long size = attrs.size();
        long modified = attrs.lastModifiedTime().toMillis();
        IndexedFile previous = known.remove(key);

        if (previous != null && previous.size() == size && previous.lastModified() == modified) {
            c.unchanged++;
            return;
        }
        if (size > maxBytes) {
            c.skipped++;
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            if (looksBinary(bytes)) {
                c.skipped++;
                return;
            }
            Document doc = new Document();
            doc.add(new StringField(LuceneStore.F_PATH, key, Field.Store.YES));
            doc.add(new StringField(LuceneStore.F_DRIVE, driveId, Field.Store.YES));
            doc.add(new TextField(LuceneStore.F_NAME, file.getFileName().toString(), Field.Store.YES));
            doc.add(new TextField(LuceneStore.F_CONTENT, new String(bytes, StandardCharsets.UTF_8), Field.Store.NO));

            writer.updateDocument(new Term(LuceneStore.F_PATH, key), doc);
            repository.upsert(new IndexedFile(key, driveId, size, modified));

            if (previous == null) {
                c.added++;
                progress.accept(new AttributedString("[NEW]     " + key,
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).toAnsi());
            } else {
                c.updated++;
                progress.accept(new AttributedString("[UPDATED]     " + key,
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)).toAnsi());
            }
        } catch (IOException | RuntimeException e) {
            c.failed++;
            progress.accept(new AttributedString("[FAILED]  " + key + " (" + e.getMessage() + ")",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)).toAnsi());
        }
    }

    private static boolean hasWantedExtension(Path file, Set<String> extensions) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && extensions.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static boolean looksBinary(byte[] bytes) {
        int limit = Math.min(bytes.length, BINARY_SNIFF_BYTES);
        for (int i = 0; i < limit; i++) {
            if (bytes[i] == 0) {
                return true;
            }
        }
        return false;
    }

    private static final class Counters {
        long added;
        long updated;
        long removed;
        long unchanged;
        long skipped;
        long failed;
    }
}
