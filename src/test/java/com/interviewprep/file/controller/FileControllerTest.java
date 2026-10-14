package com.interviewprep.file.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewprep.file.FileFixtures;
import com.interviewprep.file.config.FileStorageProperties;
import com.interviewprep.file.entity.StoredFile;
import com.interviewprep.file.mapper.StoredFileMapper;
import com.interviewprep.file.repository.StoredFileRepository;
import com.interviewprep.file.service.FileService;
import com.interviewprep.file.store.FileStorage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(FileController.class)
@Import({FileService.class, StoredFileMapper.class})
@EnableConfigurationProperties(FileStorageProperties.class)
class FileControllerTest {

    private static final String FILES = "/api/v1/files";
    private static final String PROBLEMS = "https://be-interview-prep/problems/";
    private static final UUID ID = UUID.fromString("7d3f1c9e-2a4b-4c5d-8e6f-0a1b2c3d4e5f");
    private static final Instant UPLOADED_AT = Instant.parse("2026-10-14T09:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoredFileRepository repository;

    @MockitoBean
    private FileStorage storage;

    @Test
    @DisplayName("FILE-01 uploading a PNG returns 201 with its location and metadata")
    void uploadReturnsCreated() throws Exception {
        byte[] content = FileFixtures.png();
        when(repository.saveAndFlush(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile file = invocation.getArgument(0);
            ReflectionTestUtils.setField(file, "id", ID);
            ReflectionTestUtils.setField(file, "uploadedAt", UPLOADED_AT);
            return file;
        });

        mockMvc.perform(multipart(FILES).file(new MockMultipartFile("file", "photo.png", "image/png", content)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + FILES + "/" + ID))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.originalName").value("photo.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.size").value(content.length))
                .andExpect(jsonPath("$.uploadedAt").value("2026-10-14T09:30:00Z"));

        verify(storage).write(anyString(), any());
    }

    @Test
    @DisplayName("FILE-02 an executable renamed to photo.png returns a 415 problem detail")
    void executableRenamedToPngIsRejected() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "photo.png", "image/png", FileFixtures.windowsExecutable());

        mockMvc.perform(multipart(FILES).file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(PROBLEMS + "unsupported-file-type"))
                .andExpect(jsonPath("$.title").value("Unsupported file type"))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.detail")
                        .value("Only JPEG, PNG and PDF files are accepted; the file content is none of these types"))
                .andExpect(jsonPath("$.instance").value(FILES));

        verify(storage, never()).write(anyString(), any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("FILE-02 PDF content named photo.png returns 415")
    void mismatchedExtensionIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", FileFixtures.pdf());

        mockMvc.perform(multipart(FILES).file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "unsupported-file-type"))
                .andExpect(jsonPath("$.detail")
                        .value("The file extension '.png' does not match the file content, which is application/pdf"));

        verify(storage, never()).write(anyString(), any());
    }

    @Test
    @DisplayName("FILE-03 a file over 5 MB returns a 413 problem detail")
    void oversizedFileIsRejected() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "big.png", "image/png", FileFixtures.png(5 * 1024 * 1024 + 1));

        mockMvc.perform(multipart(FILES).file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(PROBLEMS + "file-too-large"))
                .andExpect(jsonPath("$.title").value("File too large"))
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.detail")
                        .value("The file is 5242881 bytes, which exceeds the maximum allowed size of 5 MB"));

        verify(storage, never()).write(anyString(), any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("FILE-04 an empty file returns 400 empty-file")
    void emptyFileIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart(FILES).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "empty-file"))
                .andExpect(jsonPath("$.title").value("Empty file"))
                .andExpect(jsonPath("$.detail").value("The uploaded file is empty"));
    }

    @Test
    @DisplayName("FILE-04 a multipart request without the file part returns 400")
    void missingFilePartIsRejected() throws Exception {
        MockMultipartFile other = new MockMultipartFile("attachment", "photo.png", "image/png", FileFixtures.png());

        mockMvc.perform(multipart(FILES).file(other))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "bad-request"));
    }

    @Test
    @DisplayName("FILE-04 a request that is not multipart returns 415")
    void nonMultipartIsRejected() throws Exception {
        mockMvc.perform(post(FILES).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "unsupported-media-type"));
    }

    @Test
    @DisplayName("FILE-06 list returns a page of metadata with the size capped at 100")
    void listReturnsCappedPage() throws Exception {
        StoredFile file = FileFixtures.storedFile(ID, "photo.png", "image/png", 64);
        when(repository.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(file), invocation.getArgument(0), 1));

        mockMvc.perform(get(FILES).param("page", "0").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.content[0].originalName").value("photo.png"))
                .andExpect(jsonPath("$.content[0].contentType").value("image/png"))
                .andExpect(jsonPath("$.content[0].size").value(64))
                .andExpect(jsonPath("$.content[0].uploadedAt").value("2026-10-14T09:30:00Z"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("FILE-07 metadata of a stored file returns 200")
    void getReturnsMetadata() throws Exception {
        when(repository.findById(ID))
                .thenReturn(Optional.of(FileFixtures.storedFile(ID, "report.pdf", "application/pdf", 120)));

        mockMvc.perform(get(FILES + "/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.originalName").value("report.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.size").value(120));
    }

    @Test
    @DisplayName("FILE-07 metadata of an unknown file returns a 404 problem detail")
    void getUnknownReturnsNotFound() throws Exception {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(FILES + "/" + ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(PROBLEMS + "not-found"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("File " + ID + " was not found"));
    }

    @Test
    @DisplayName("FILE-07 a malformed id returns 400")
    void malformedIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get(FILES + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "bad-request"));
    }

    @Test
    @DisplayName("FILE-08 download returns the bytes as an attachment with the original name")
    void downloadReturnsAttachment() throws Exception {
        byte[] content = FileFixtures.pdf();
        StoredFile file = FileFixtures.storedFile(ID, "Quarterly report é.pdf", "application/pdf", content.length);
        when(repository.findById(ID)).thenReturn(Optional.of(file));
        when(storage.read(file.getStoredName())).thenReturn(new ByteArrayResource(content));

        MvcResult result = mockMvc.perform(get(FILES + "/" + ID + "/content"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(content))
                .andReturn();

        ContentDisposition disposition =
                ContentDisposition.parse(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION));
        assertThat(disposition.isAttachment()).isTrue();
        assertThat(disposition.getFilename()).isEqualTo("Quarterly report é.pdf");
    }

    @Test
    @DisplayName("FILE-08 download of an unknown file returns 404")
    void downloadUnknownReturnsNotFound() throws Exception {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(FILES + "/" + ID + "/content"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "not-found"));

        verify(storage, never()).read(anyString());
    }

    @Test
    @DisplayName("FILE-09 delete returns 204 and removes the record and the stored content")
    void deleteReturnsNoContent() throws Exception {
        StoredFile file = FileFixtures.storedFile(ID, "photo.png", "image/png", 64);
        when(repository.findById(ID)).thenReturn(Optional.of(file));

        mockMvc.perform(delete(FILES + "/" + ID)).andExpect(status().isNoContent());

        verify(repository).delete(file);
        verify(storage).delete(file.getStoredName());
    }

    @Test
    @DisplayName("FILE-09 deleting an unknown file returns 404")
    void deleteUnknownReturnsNotFound() throws Exception {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        mockMvc.perform(delete(FILES + "/" + ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(PROBLEMS + "not-found"));

        verify(storage, never()).delete(anyString());
    }
}
