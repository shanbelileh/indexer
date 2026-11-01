package com.example.fileindexer.store;

import com.example.fileindexer.model.IndexedFile;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tracks what has been indexed so later runs only touch new or changed files. */
@Repository
public class IndexedFileRepository {

    private final JdbcClient jdbc;

    public IndexedFileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** All known files of a drive, keyed by path. */
    public Map<String, IndexedFile> findByDrive(String drive) {
        Map<String, IndexedFile> result = new HashMap<>();
        jdbc.sql("SELECT file_path, file_size, last_modified FROM indexed_file WHERE drive_root = :drive")
                .param("drive", drive)
                .query((rs, i) -> new IndexedFile(rs.getString(1), drive, rs.getLong(2), rs.getLong(3)))
                .list()
                .forEach(f -> result.put(f.path(), f));
        return result;
    }

    public void upsert(IndexedFile f) {
        jdbc.sql("""
                        MERGE INTO indexed_file (file_path, drive_root, file_size, last_modified, indexed_at)
                        KEY (file_path)
                        VALUES (:path, :drive, :size, :lastModified, CURRENT_TIMESTAMP)
                        """)
                .param("path", f.path())
                .param("drive", f.drive())
                .param("size", f.size())
                .param("lastModified", f.lastModified())
                .update();
    }

    public void delete(String path) {
        jdbc.sql("DELETE FROM indexed_file WHERE file_path = :path").param("path", path).update();
    }

    public long countByDrive(String drive) {
        return jdbc.sql("SELECT COUNT(*) FROM indexed_file WHERE drive_root = :drive")
                .param("drive", drive)
                .query(Long.class)
                .single();
    }
}
