package com.interviewprep.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileUploadOverHttpTest {

    private static final int FIVE_MEGABYTES = 5 * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @TempDir
    static Path storageRoot;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void storageLocation(DynamicPropertyRegistry registry) {
        registry.add("file-storage.location", () -> storageRoot.toString());
    }

    @Test
    @DisplayName("FILE-03 a real HTTP upload one byte over 5 MB gets the file-too-large problem detail")
    void oversizedUploadOverHttpReturnsFileTooLarge() throws Exception {
        byte[] content = new byte[FIVE_MEGABYTES + 1];
        System.arraycopy(PNG_SIGNATURE, 0, content, 0, PNG_SIGNATURE.length);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/files", multipartBody(content), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        JsonNode problem = objectMapper.readTree(response.getBody());
        assertThat(problem.get("type").asText()).endsWith("/file-too-large");
        assertThat(problem.get("status").asInt()).isEqualTo(413);
    }

    private HttpEntity<MultiValueMap<String, Object>> multipartBody(byte[] content) {
        ByteArrayResource file = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "large.png";
            }
        };
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(parts, headers);
    }
}
