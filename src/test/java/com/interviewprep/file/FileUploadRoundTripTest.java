package com.interviewprep.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.file.repository.StoredFileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadRoundTripTest {

    private static final String FILES = "/api/v1/files";
    private static final String ORIGINAL_NAME = "Quarterly report é.pdf";

    @TempDir
    static Path storageRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoredFileRepository repository;

    @DynamicPropertySource
    static void storageLocation(DynamicPropertyRegistry registry) {
        registry.add("file-storage.location", () -> storageRoot.toString());
    }

    @Test
    @DisplayName("FILE-10 upload, list, download and delete round trip through disk and database")
    void uploadListDownloadDelete() throws Exception {
        byte[] bytes = FileFixtures.pdf();
        MockMultipartFile upload = new MockMultipartFile("file", ORIGINAL_NAME, "application/octet-stream", bytes);

        MvcResult created = mockMvc.perform(multipart(FILES).file(upload))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        UUID id = UUID.fromString(body.get("id").asText());

        assertThat(created.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(FILES + "/" + id);
        assertThat(body.get("originalName").asText()).isEqualTo(ORIGINAL_NAME);
        assertThat(body.get("contentType").asText()).isEqualTo("application/pdf");
        assertThat(body.get("size").asLong()).isEqualTo(bytes.length);
        assertThat(body.get("uploadedAt").asText()).isNotBlank();
        assertThat(storedFiles())
                .singleElement()
                .satisfies(path -> assertThat(Files.readAllBytes(path)).isEqualTo(bytes));

        mockMvc.perform(get(FILES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].originalName").value(ORIGINAL_NAME));

        MvcResult downloaded = mockMvc.perform(get(FILES + "/" + id + "/content"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        ContentDisposition disposition =
                ContentDisposition.parse(downloaded.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION));
        assertThat(disposition.isAttachment()).isTrue();
        assertThat(disposition.getFilename()).isEqualTo(ORIGINAL_NAME);
        assertThat(downloaded.getResponse().getContentAsByteArray()).isEqualTo(bytes);

        mockMvc.perform(delete(FILES + "/" + id)).andExpect(status().isNoContent());

        assertThat(storedFiles()).isEmpty();
        assertThat(repository.findById(id)).isEmpty();
        mockMvc.perform(get(FILES + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get(FILES + "/" + id + "/content")).andExpect(status().isNotFound());
    }

    private List<Path> storedFiles() throws IOException {
        if (!Files.isDirectory(storageRoot)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(storageRoot)) {
            return files.toList();
        }
    }
}
