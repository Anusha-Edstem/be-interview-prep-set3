package com.interviewprep.file.service;

import com.interviewprep.file.config.FileStorageProperties;
import com.interviewprep.file.dto.StoredFileResponse;
import com.interviewprep.file.entity.StoredFile;
import com.interviewprep.file.error.EmptyFileException;
import com.interviewprep.file.error.FileStorageException;
import com.interviewprep.file.error.FileTooLargeException;
import com.interviewprep.file.error.UnsupportedFileTypeException;
import com.interviewprep.file.mapper.StoredFileMapper;
import com.interviewprep.file.repository.StoredFileRepository;
import com.interviewprep.file.store.FileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class FileService {

    private final StoredFileRepository repository;
    private final FileStorage storage;
    private final StoredFileMapper mapper;
    private final DataSize maxFileSize;

    public FileService(
            StoredFileRepository repository,
            FileStorage storage,
            StoredFileMapper mapper,
            FileStorageProperties properties) {
        this.repository = repository;
        this.storage = storage;
        this.mapper = mapper;
        this.maxFileSize = properties.maxFileSize();
    }

    public StoredFileResponse upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new EmptyFileException();
        }
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new FileTooLargeException(file.getSize(), maxFileSize);
        }
        String originalName = FileNames.sanitize(file.getOriginalFilename());
        FileType type = detectType(file);
        requireMatchingExtension(originalName, type);

        String storedName = UUID.randomUUID().toString();
        storage.write(storedName, file);
        StoredFile saved = saveOrDiscard(new StoredFile(originalName, storedName, type.mediaType(), file.getSize()));
        return mapper.toResponse(saved);
    }

    private FileType detectType(MultipartFile file) {
        byte[] header;
        try (InputStream content = file.getInputStream()) {
            header = content.readNBytes(FileType.SIGNATURE_LENGTH);
        } catch (IOException ex) {
            throw new FileStorageException("The uploaded file could not be read", ex);
        }
        return FileType.detect(header)
                .orElseThrow(() -> new UnsupportedFileTypeException(
                        "Only JPEG, PNG and PDF files are accepted; the file content is none of these types"));
    }

    private void requireMatchingExtension(String originalName, FileType type) {
        FileNames.extension(originalName)
                .filter(extension -> !type.acceptsExtension(extension))
                .ifPresent(extension -> {
                    throw new UnsupportedFileTypeException("The file extension '." + extension
                            + "' does not match the file content, which is " + type.mediaType());
                });
    }

    private StoredFile saveOrDiscard(StoredFile file) {
        try {
            return repository.saveAndFlush(file);
        } catch (RuntimeException ex) {
            discardContent(file.getStoredName(), ex);
            throw ex;
        }
    }

    private void discardContent(String storedName, RuntimeException failure) {
        try {
            storage.delete(storedName);
        } catch (FileStorageException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}
