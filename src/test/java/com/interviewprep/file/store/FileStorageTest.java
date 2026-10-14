package com.interviewprep.file.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import com.interviewprep.file.config.FileStorageProperties;
import com.interviewprep.file.error.FileStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
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
        root = tempDir.resolve("outer/inner/storage").toAbsolutePath().normalize();
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

        assertThat(tempDir.resolve("outer/evil.txt")).doesNotExist();
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
        Path outside = Files.writeString(
                Files.createDirectories(tempDir.resolve("outer/inner")).resolve("keep.txt"), "keep");

        assertThatThrownBy(() -> storage.delete("../keep.txt")).isInstanceOf(FileStorageException.class);

        assertThat(outside).exists();
    }

    @Test
    @DisplayName("FILE-01 a stream that fails mid-copy raises a storage error and leaves no partial file")
    void failedWriteRemovesPartialFile() throws IOException {
        String storedName = UUID.randomUUID().toString();
        Path target = storage.resolve(storedName);
        AtomicBoolean partialFileExisted = new AtomicBoolean();

        assertThatThrownBy(() -> storage.write(storedName, failingAfter(1024, target, partialFileExisted)))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("The file could not be processed by storage")
                .cause()
                .isInstanceOf(IOException.class)
                .hasMessage("connection reset");

        assertThat(partialFileExisted).isTrue();
        assertThat(target).doesNotExist();
        try (Stream<Path> files = Files.list(root)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    @DisplayName("FILE-01 a failure to remove the partial file is attached to the write failure")
    void failedCleanupIsSuppressed() {
        String storedName = UUID.randomUUID().toString();
        Path target = storage.resolve(storedName);
        IOException cleanupFailure = new IOException("file locked");

        try (MockedStatic<Files> files = mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.deleteIfExists(target)).thenThrow(cleanupFailure);

            assertThatThrownBy(() -> storage.write(storedName, failingAfter(1024, target, new AtomicBoolean())))
                    .isInstanceOf(FileStorageException.class)
                    .cause()
                    .hasMessage("connection reset")
                    .hasSuppressedException(cleanupFailure);
        }
    }

    @Test
    @DisplayName("FILE-09 a delete that the file system refuses raises a storage error")
    void failedDeleteRaisesStorageError() throws IOException {
        String storedName = UUID.randomUUID().toString();
        Path blocking = Files.createDirectories(root.resolve(storedName));
        Files.writeString(blocking.resolve("child.txt"), "keeps the directory non-empty");

        assertThatThrownBy(() -> storage.delete(storedName))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("The file could not be processed by storage")
                .hasCauseInstanceOf(DirectoryNotEmptyException.class);

        assertThat(blocking).isDirectory();
    }

    @Test
    @DisplayName("FILE-05 a name the file system cannot represent is rejected")
    void invalidPathIsRejected() {
        assertThatThrownBy(() -> storage.resolve("evil\u0000.txt"))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("The file could not be located in storage");
    }

    private static InputStreamSource failingAfter(int byteCount, Path target, AtomicBoolean partialFileExisted) {
        return () -> new InputStream() {
            private int served;

            @Override
            public int read() throws IOException {
                if (served == byteCount) {
                    partialFileExisted.set(Files.exists(target));
                    throw new IOException("connection reset");
                }
                served++;
                return 'x';
            }
        };
    }
}
