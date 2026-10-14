package com.interviewprep.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

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
}
