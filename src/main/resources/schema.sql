CREATE TABLE IF NOT EXISTS indexed_file (
    file_path     VARCHAR(2048) PRIMARY KEY,
    drive_root    VARCHAR(64)   NOT NULL,
    file_size     BIGINT        NOT NULL,
    last_modified BIGINT        NOT NULL,
    indexed_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_indexed_file_drive ON indexed_file (drive_root);
