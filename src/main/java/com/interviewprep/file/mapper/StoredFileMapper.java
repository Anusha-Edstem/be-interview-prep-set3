package com.interviewprep.file.mapper;

import com.interviewprep.file.dto.StoredFileResponse;
import com.interviewprep.file.entity.StoredFile;
import org.springframework.stereotype.Component;

@Component
public class StoredFileMapper {

    public StoredFileResponse toResponse(StoredFile file) {
        return new StoredFileResponse(
                file.getId(), file.getOriginalName(), file.getContentType(), file.getSize(), file.getUploadedAt());
    }
}
