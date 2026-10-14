package com.interviewprep.library.repository;

import com.interviewprep.library.entity.Book;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, UUID> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, UUID id);

    @Query("""
            select b from Book b
            where lower(b.title) like :titlePattern escape '!'
              and lower(b.author) like :authorPattern escape '!'
            """)
    Page<Book> search(
            @Param("titlePattern") String titlePattern,
            @Param("authorPattern") String authorPattern,
            Pageable pageable);
}
