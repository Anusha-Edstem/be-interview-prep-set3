package com.interviewprep.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.interviewprep.file.FileFixtures;
import com.interviewprep.file.config.FileStorageProperties;
import com.interviewprep.file.dto.FileDownload;
import com.interviewprep.file.dto.StoredFileResponse;
import com.interviewprep.file.entity.StoredFile;
import com.interviewprep.file.error.EmptyFileException;
import com.interviewprep.file.error.FileStorageException;
import com.interviewprep.file.error.FileTooLargeException;
import com.interviewprep.file.error.UnsupportedFileTypeException;
import com.interviewprep.file.mapper.StoredFileMapper;
import com.interviewprep.file.repository.StoredFileRepository;
import com.interviewprep.file.store.FileStorage;
import com.interviewprep.shared.error.NotFoundException;
import com.interviewprep.shared.web.PageResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final Instant UPLOADED_AT = Instant.parse("2026-10-14T09:30:00Z");
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    @TempDir
    private Path tempDir;

    @Mock
    private StoredFileRepository repository;

    private Path root;
    private FileStorage storage;
    private FileService service;

    @BeforeEach
    void setUp() {
        root = tempDir.resolve("outer/inner/storage").toAbsolutePath().normalize();
        FileStorageProperties properties = new FileStorageProperties(root.toString(), DataSize.ofMegabytes(5));
        storage = new FileStorage(properties);
        service = new FileService(repository, storage, new StoredFileMapper(), properties);
    }

    @Test
    @DisplayName("FILE-06 list caps the page size at 100 and orders newest first")
    void listCapsPageSizeAndSortsNewestFirst() {
        StoredFile file = FileFixtures.storedFile(UUID.randomUUID(), "a.png", "image/png", 64);
        when(repository.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(file), invocation.getArgument(0), 1));

        PageResponse<StoredFileResponse> page = service.list(PageRequest.of(2, 500, Sort.by("originalName")));

        ArgumentCaptor<Pageable> requested = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(requested.capture());
        assertThat(requested.getValue().getPageNumber()).isEqualTo(2);
        assertThat(requested.getValue().getPageSize()).isEqualTo(FileService.MAX_PAGE_SIZE);
        assertThat(requested.getValue().getSort())
                .containsExactly(Sort.Order.desc("uploadedAt"), Sort.Order.desc("id"));
        assertThat(page.size()).isEqualTo(100);
        assertThat(page.content()).extracting(StoredFileResponse::originalName).containsExactly("a.png");
    }

    @Test
    @DisplayName("FILE-07 metadata of an unknown file raises not found")
    void getUnknownFile() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("File " + id + " was not found");
    }

    @Test
    @DisplayName("FILE-08 download returns the stored bytes with the original name and detected type")
    void downloadReturnsStoredContent() throws IOException {
        byte[] content = FileFixtures.pdf();
        StoredFile file = FileFixtures.storedFile(UUID.randomUUID(), "report.pdf", "application/pdf", content.length);
        storage.write(file.getStoredName(), new ByteArrayResource(content));
        when(repository.findById(file.getId())).thenReturn(Optional.of(file));

        FileDownload download = service.download(file.getId());

        assertThat(download.originalName()).isEqualTo("report.pdf");
        assertThat(download.contentType()).isEqualTo("application/pdf");
        assertThat(download.size()).isEqualTo(content.length);
        assertThat(download.content().getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    @DisplayName("FILE-08 download of an unknown file raises not found")
    void downloadUnknownFile() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("FILE-09 delete removes both the record and the stored content")
    void deleteRemovesRecordAndContent() throws IOException {
        StoredFile file = FileFixtures.storedFile(UUID.randomUUID(), "a.png", "image/png", 64);
        storage.write(file.getStoredName(), new ByteArrayResource(FileFixtures.png()));
        when(repository.findById(file.getId())).thenReturn(Optional.of(file));

        service.delete(file.getId());

        verify(repository).delete(file);
        verify(repository).flush();
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("FILE-09 deleting an unknown file raises not found and deletes nothing")
    void deleteUnknownFile() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NotFoundException.class);

        verify(repository, never()).delete(any(StoredFile.class));
    }

    @Test
    @DisplayName("FILE-03 a file one byte over 5 MB is rejected before anything is stored")
    void oversizedFileIsRejected() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", FileFixtures.png(MAX_BYTES + 1));

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessage("The file is 5242881 bytes, which exceeds the maximum allowed size of 5 MB");

        verifyNoInteractions(repository);
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("FILE-03 a file of exactly 5 MB is accepted")
    void fileAtLimitIsAccepted() {
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", FileFixtures.png(MAX_BYTES));
        stubSave();

        StoredFileResponse response = service.upload(file);

        assertThat(response.size()).isEqualTo(MAX_BYTES);
    }

    @Test
    @DisplayName("FILE-01 upload stores the content under a server-generated name and records the detected type")
    void uploadStoresContentAndRecord() throws IOException {
        byte[] content = FileFixtures.jpeg();
        MockMultipartFile file = new MockMultipartFile("file", "holiday.jpg", "text/plain", content);
        stubSave();

        StoredFileResponse response = service.upload(file);

        ArgumentCaptor<StoredFile> saved = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).saveAndFlush(saved.capture());
        String storedName = saved.getValue().getStoredName();
        assertThat(UUID.fromString(storedName)).isNotNull();
        assertThat(storedFiles()).containsExactly(root.resolve(storedName));
        assertThat(Files.readAllBytes(root.resolve(storedName))).isEqualTo(content);
        assertThat(response.id()).isNotNull();
        assertThat(response.originalName()).isEqualTo("holiday.jpg");
        assertThat(response.contentType()).isEqualTo("image/jpeg");
        assertThat(response.size()).isEqualTo(content.length);
        assertThat(response.uploadedAt()).isEqualTo(UPLOADED_AT);
    }

    @Test
    @DisplayName("FILE-01 upload detects PNG and PDF content")
    void uploadDetectsPngAndPdf() {
        stubSave();

        StoredFileResponse png = service.upload(new MockMultipartFile("file", "a.png", null, FileFixtures.png()));
        StoredFileResponse pdf = service.upload(new MockMultipartFile("file", "b.PDF", null, FileFixtures.pdf()));

        assertThat(png.contentType()).isEqualTo("image/png");
        assertThat(pdf.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("FILE-02 an executable renamed to photo.png is rejected and nothing is stored")
    void executableRenamedToPngIsRejected() throws IOException {
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.png", "image/png", FileFixtures.windowsExecutable());

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessage("Only JPEG, PNG and PDF files are accepted; the file content is none of these types");

        verifyNoInteractions(repository);
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("FILE-02 an extension that does not match the content is rejected")
    void mismatchedExtensionIsRejected() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", FileFixtures.pdf());

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessage("The file extension '.png' does not match the file content, which is application/pdf");

        verifyNoInteractions(repository);
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("FILE-02 a name without an extension is accepted on its content alone")
    void nameWithoutExtensionIsAccepted() {
        stubSave();

        StoredFileResponse response = service.upload(new MockMultipartFile("file", "scan", null, FileFixtures.pdf()));

        assertThat(response.originalName()).isEqualTo("scan");
        assertThat(response.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("FILE-04 an empty file is rejected")
    void emptyFileIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(EmptyFileException.class)
                .hasMessage("The uploaded file is empty");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("FILE-05 a traversal original name is kept for display only and the content stays inside storage")
    void traversalNameNeverEscapesStorage() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "../../evil.png", "image/png", FileFixtures.png());
        stubSave();

        StoredFileResponse response = service.upload(file);

        assertThat(response.originalName()).isEqualTo("evil.png");
        assertThat(storedFiles())
                .hasSize(1)
                .allSatisfy(path -> assertThat(path.getParent()).isEqualTo(root));
        assertThat(tempDir.resolve("evil.png")).doesNotExist();
        assertThat(tempDir.resolve("outer/evil.png")).doesNotExist();
        try (Stream<Path> everything = Files.walk(tempDir)) {
            assertThat(everything.filter(Files::isRegularFile))
                    .allSatisfy(path -> assertThat(path).startsWithRaw(root));
        }
    }

    @Test
    @DisplayName("FILE-01 when the record cannot be saved the written content is removed")
    void failedSaveRemovesContent() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", FileFixtures.png());
        DataIntegrityViolationException failure = new DataIntegrityViolationException("duplicate");
        when(repository.saveAndFlush(any(StoredFile.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.upload(file)).isSameAs(failure);

        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("FILE-01 an upload stream that cannot be read raises a storage error and stores nothing")
    void unreadableUploadRaisesStorageError() throws IOException {
        IOException readFailure = new IOException("stream closed");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(64L);
        when(file.getOriginalFilename()).thenReturn("photo.png");
        when(file.getInputStream()).thenThrow(readFailure);

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("The uploaded file could not be read")
                .hasCause(readFailure);

        verifyNoInteractions(repository);
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("FILE-01 a failed cleanup after a failed save is attached to the original database error")
    void failedCleanupIsSuppressedOnSaveFailure() {
        FileStorage failingStorage = mock(FileStorage.class);
        FileService failingService = new FileService(
                repository,
                failingStorage,
                new StoredFileMapper(),
                new FileStorageProperties(root.toString(), DataSize.ofMegabytes(5)));
        DataIntegrityViolationException failure = new DataIntegrityViolationException("duplicate");
        FileStorageException cleanupFailure = new FileStorageException("The file could not be processed by storage");
        when(repository.saveAndFlush(any(StoredFile.class))).thenThrow(failure);
        doThrow(cleanupFailure).when(failingStorage).delete(anyString());
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", FileFixtures.png());

        assertThatThrownBy(() -> failingService.upload(file))
                .isSameAs(failure)
                .satisfies(thrown -> assertThat(thrown.getSuppressed()).containsExactly(cleanupFailure));

        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        verify(failingStorage).write(written.capture(), any());
        verify(failingStorage).delete(written.getValue());
    }

    private void stubSave() {
        when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile file = invocation.getArgument(0);
            ReflectionTestUtils.setField(file, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(file, "uploadedAt", UPLOADED_AT);
            return file;
        });
    }

    private List<Path> storedFiles() throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(root)) {
            return files.toList();
        }
    }
}
