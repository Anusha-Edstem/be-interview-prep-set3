package com.interviewprep.library.mapper;

import com.interviewprep.library.dto.BookResponse;
import com.interviewprep.library.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublishedYear(),
                !book.isBorrowed(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }
}
