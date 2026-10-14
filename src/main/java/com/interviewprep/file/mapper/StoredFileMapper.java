package com.interviewprep.file.mapper;

import com.interviewprep.file.dto.FileDownload;
import com.interviewprep.file.dto.StoredFileResponse;
import com.interviewprep.file.entity.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class StoredFileMapper {

    public StoredFileResponse toResponse(StoredFile file) {
        return new StoredFileResponse(
                file.getId(), file.getOriginalName(), file.getContentType(), file.getSize(), file.getUploadedAt());
    }

    public FileDownload toDownload(StoredFile file, Resource content) {
        return new FileDownload(file.getOriginalName(), file.getContentType(), file.getSize(), content);
    }
}
