CREATE TABLE stored_files (
    id UUID NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(36) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_stored_files PRIMARY KEY (id),
    CONSTRAINT uk_stored_files_stored_name UNIQUE (stored_name),
    CONSTRAINT ck_stored_files_size_bytes CHECK (size_bytes > 0)
);

CREATE INDEX idx_stored_files_uploaded_at ON stored_files (uploaded_at);
