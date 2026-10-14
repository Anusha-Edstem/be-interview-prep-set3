package com.interviewprep.file.repository;

import com.interviewprep.file.entity.StoredFile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {}
