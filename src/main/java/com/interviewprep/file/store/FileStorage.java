package com.interviewprep.file.store;

import com.interviewprep.file.config.FileStorageProperties;
import com.interviewprep.file.error.FileStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class FileStorage {

    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);

    private final Path root;

    public FileStorage(FileStorageProperties properties) {
        this.root = Path.of(properties.location()).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public void write(String storedName, InputStreamSource source) {
        Path target = resolve(storedName);
        try (InputStream content = source.getInputStream()) {
            Files.createDirectories(root);
            Files.copy(content, target);
        } catch (FileAlreadyExistsException ex) {
            throw failure("write", storedName, ex);
        } catch (IOException ex) {
            removePartialWrite(target, ex);
            throw failure("write", storedName, ex);
        }
    }

    public Resource read(String storedName) {
        Path target = resolve(storedName);
        if (!Files.isRegularFile(target)) {
            log.error("Stored content {} is missing from storage", storedName);
            throw new FileStorageException("The stored content of this file is missing");
        }
        return new FileSystemResource(target);
    }

    public void delete(String storedName) {
        Path target = resolve(storedName);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw failure("delete", storedName, ex);
        }
    }

    Path resolve(String storedName) {
        Path target;
        try {
            target = root.resolve(storedName).normalize();
        } catch (InvalidPathException ex) {
            throw outsideRoot();
        }
        if (!target.startsWith(root) || target.equals(root)) {
            throw outsideRoot();
        }
        return target;
    }

    private void removePartialWrite(Path target, IOException failure) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    private FileStorageException failure(String operation, String storedName, IOException cause) {
        log.error("File storage {} failed for {}", operation, storedName, cause);
        return new FileStorageException("The file could not be processed by storage", cause);
    }

    private FileStorageException outsideRoot() {
        log.warn("Rejected a storage path outside the storage location");
        return new FileStorageException("The file could not be located in storage");
    }
}
