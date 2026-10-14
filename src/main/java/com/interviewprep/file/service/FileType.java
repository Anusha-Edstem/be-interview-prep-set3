package com.interviewprep.file.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum FileType {
    JPEG("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, Set.of("jpg", "jpeg")),
    PNG("image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, Set.of("png")),
    PDF("application/pdf", "%PDF-".getBytes(StandardCharsets.US_ASCII), Set.of("pdf"));

    public static final int SIGNATURE_LENGTH = 8;

    private final String mediaType;
    private final byte[] signature;
    private final Set<String> extensions;

    FileType(String mediaType, byte[] signature, Set<String> extensions) {
        this.mediaType = mediaType;
        this.signature = signature;
        this.extensions = extensions;
    }

    public String mediaType() {
        return mediaType;
    }

    public boolean acceptsExtension(String extension) {
        return extensions.contains(extension);
    }

    public static Optional<FileType> detect(byte[] header) {
        return Arrays.stream(values()).filter(type -> type.matches(header)).findFirst();
    }

    private boolean matches(byte[] header) {
        return header.length >= signature.length
                && Arrays.equals(header, 0, signature.length, signature, 0, signature.length);
    }
}
