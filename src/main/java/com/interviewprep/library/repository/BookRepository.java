package com.interviewprep.library.repository;

import com.interviewprep.library.entity.Book;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, UUID> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, UUID id);
}
