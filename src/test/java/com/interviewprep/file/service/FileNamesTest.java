package com.interviewprep.file.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FileNamesTest {

    @ParameterizedTest
    @CsvSource({
        "../../evil.txt, evil.txt",
        "..\\..\\windows\\evil.png, evil.png",
        "/etc/passwd, passwd",
        "C:\\Users\\me\\photo.jpg, photo.jpg",
        "dir/sub\\mixed.pdf, mixed.pdf",
        "report.pdf, report.pdf",
        "'  spaced name.png  ', spaced name.png"
    })
    @DisplayName("FILE-05 path components are stripped from the original name")
    void stripsPathComponents(String rawName, String expected) {
        assertThat(FileNames.sanitize(rawName)).isEqualTo(expected);
    }

    @Test
    @DisplayName("FILE-05 control characters are removed from the original name")
    void stripsControlCharacters() {
        String sanitized = FileNames.sanitize("inv\r\noice\u0000.pdf\t");

        assertThat(sanitized).isEqualTo("invoice.pdf");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"..", ".", "../", "dir/", "\u0007\u0008", "   "})
    @DisplayName("FILE-05 a name with nothing usable falls back to file")
    void fallsBackWhenNothingIsLeft(String rawName) {
        assertThat(FileNames.sanitize(rawName)).isEqualTo("file");
    }

    @Test
    @DisplayName("FILE-05 an overlong name is capped at 255 characters")
    void capsLength() {
        String sanitized = FileNames.sanitize("a".repeat(300) + ".png");

        assertThat(sanitized).hasSize(255);
    }

    @Test
    @DisplayName("FILE-05 capping never splits a surrogate pair")
    void capKeepsSurrogatePairsWhole() {
        String sanitized = FileNames.sanitize("a".repeat(254) + "\uD83D\uDE00" + "tail.png");

        assertThat(sanitized).hasSize(254).isEqualTo("a".repeat(254));
    }

    @ParameterizedTest
    @CsvSource({"photo.PNG, png", "archive.tar.pdf, pdf", "scan.JpEg, jpeg"})
    @DisplayName("FILE-02 the extension is the lower-cased text after the last dot")
    void extractsExtension(String name, String expected) {
        assertThat(FileNames.extension(name)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"README", "trailing.", "file"})
    @DisplayName("FILE-02 a name without a usable extension has none")
    void noExtension(String name) {
        assertThat(FileNames.extension(name)).isEmpty();
    }
}
