package com.interviewprep.file.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewprep.file.config.FileStorageProperties;
import com.interviewprep.file.error.FileStorageException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.unit.DataSize;

class FileStorageTest {

    private static final byte[] CONTENT = "%PDF-1.7 sample".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    private Path tempDir;

    private Path root;
    private FileStorage storage;

    @BeforeEach
    void setUp() {
        root = tempDir.resolve("storage").toAbsolutePath().normalize();
        storage = new FileStorage(new FileStorageProperties(root.toString(), DataSize.ofMegabytes(5)));
    }

    @Test
    @DisplayName("FILE-05 a server-generated name resolves directly inside the storage location")
    void storedNameResolvesInsideRoot() {
        String storedName = UUID.randomUUID().toString();

        Path resolved = storage.resolve(storedName);

        assertThat(resolved).startsWithRaw(root);
        assertThat(resolved.getParent()).isEqualTo(root);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../evil.txt", "../evil.txt", "..", ".", "", "a/../../evil.txt"})
    @DisplayName("FILE-05 a name that climbs out of the storage location is rejected")
    void traversalNameIsRejected(String storedName) {
        assertThatThrownBy(() -> storage.resolve(storedName))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("The file could not be located in storage");
    }

    @Test
    @DisplayName("FILE-05 an absolute path never escapes the storage location")
    void absolutePathIsRejected() {
        String absolute = tempDir.resolve("evil.txt").toAbsolutePath().toString();

        assertThatThrownBy(() -> storage.resolve(absolute)).isInstanceOf(FileStorageException.class);
    }

    @Test
    @DisplayName("FILE-05 writing with a traversal name creates nothing anywhere")
    void traversalWriteCreatesNothing() throws IOException {
        ByteArrayResource source = new ByteArrayResource(CONTENT);

        assertThatThrownBy(() -> storage.write("../../evil.txt", source)).isInstanceOf(FileStorageException.class);

        assertThat(tempDir.resolve("evil.txt")).doesNotExist();
        assertThat(tempDir.getParent().resolve("evil.txt")).doesNotExist();
        try (Stream<Path> files = Files.walk(tempDir)) {
            assertThat(files.filter(Files::isRegularFile)).isEmpty();
        }
    }

    @Test
    @DisplayName("FILE-08 written content reads back byte for byte")
    void writeThenRead() throws IOException {
        String storedName = UUID.randomUUID().toString();

        storage.write(storedName, new ByteArrayResource(CONTENT));
        Resource resource = storage.read(storedName);

        assertThat(resource.getContentAsByteArray()).isEqualTo(CONTENT);
        assertThat(root.resolve(storedName)).exists();
    }

    @Test
    @DisplayName("FILE-01 writing never overwrites existing content")
    void writeKeepsExistingContent() throws IOException {
        String storedName = UUID.randomUUID().toString();
        storage.write(storedName, new ByteArrayResource(CONTENT));
        ByteArrayResource replacement = new ByteArrayResource(new byte[] {1, 2, 3});

        assertThatThrownBy(() -> storage.write(storedName, replacement)).isInstanceOf(FileStorageException.class);

        assertThat(Files.readAllBytes(root.resolve(storedName))).isEqualTo(CONTENT);
    }

    @Test
    @DisplayName("FILE-08 reading content missing from disk is a storage error")
    void readMissingContent() {
        String storedName = UUID.randomUUID().toString();

        assertThatThrownBy(() -> storage.read(storedName))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("The stored content of this file is missing");
    }

    @Test
    @DisplayName("FILE-09 delete removes the stored content")
    void deleteRemovesContent() {
        String storedName = UUID.randomUUID().toString();
        storage.write(storedName, new ByteArrayResource(CONTENT));

        storage.delete(storedName);

        assertThat(root.resolve(storedName)).doesNotExist();
    }

    @Test
    @DisplayName("FILE-09 delete with a traversal name touches nothing outside the storage location")
    void traversalDeleteIsRejected() throws IOException {
        Path outside = Files.writeString(tempDir.resolve("keep.txt"), "keep");

        assertThatThrownBy(() -> storage.delete("../keep.txt")).isInstanceOf(FileStorageException.class);

        assertThat(outside).exists();
    }
}
