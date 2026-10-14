package com.interviewprep.file;

import com.interviewprep.file.entity.StoredFile;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public final class FileFixtures {

    public static final Instant UPLOADED_AT = Instant.parse("2026-10-14T09:30:00Z");

    public static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private FileFixtures() {}

    public static byte[] png() {
        return withSignature(PNG_SIGNATURE, 64);
    }

    public static byte[] png(int size) {
        return withSignature(PNG_SIGNATURE, size);
    }

    public static byte[] jpeg() {
        return withSignature(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 64);
    }

    public static byte[] pdf() {
        return "%PDF-1.7\n1 0 obj << /Type /Catalog >> endobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }

    public static byte[] windowsExecutable() {
        return withSignature(new byte[] {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00}, 64);
    }

    public static StoredFile storedFile(UUID id, String originalName, String contentType, long size) {
        StoredFile file = new StoredFile(originalName, UUID.randomUUID().toString(), contentType, size);
        ReflectionTestUtils.setField(file, "id", id);
        ReflectionTestUtils.setField(file, "uploadedAt", UPLOADED_AT);
        return file;
    }

    private static byte[] withSignature(byte[] signature, int size) {
        byte[] content = Arrays.copyOf(signature, Math.max(size, signature.length));
        Arrays.fill(content, signature.length, content.length, (byte) 0x2A);
        return content;
    }
}
