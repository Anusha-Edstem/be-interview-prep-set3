package com.interviewprep.file.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("file-storage")
public record FileStorageProperties(
        @NotBlank String location, @NotNull DataSize maxFileSize) {}
