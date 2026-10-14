package com.interviewprep.file.dto;

import org.springframework.core.io.Resource;

public record FileDownload(String originalName, String contentType, long size, Resource content) {}
