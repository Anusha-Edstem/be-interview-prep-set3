package com.interviewprep.file.service;

import java.util.Locale;
import java.util.Optional;

public final class FileNames {

    public static final String FALLBACK = "file";
    public static final int MAX_LENGTH = 255;

    private FileNames() {}

    public static String sanitize(String rawName) {
        if (rawName == null) {
            return FALLBACK;
        }
        String baseName = rawName.substring(Math.max(rawName.lastIndexOf('/'), rawName.lastIndexOf('\\')) + 1);
        String printable = baseName.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .strip();
        if (printable.isEmpty() || printable.equals(".") || printable.equals("..")) {
            return FALLBACK;
        }
        return truncate(printable);
    }

    public static Optional<String> extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String truncate(String name) {
        if (name.length() <= MAX_LENGTH) {
            return name;
        }
        int end = Character.isHighSurrogate(name.charAt(MAX_LENGTH - 1)) ? MAX_LENGTH - 1 : MAX_LENGTH;
        return name.substring(0, end);
    }
}
