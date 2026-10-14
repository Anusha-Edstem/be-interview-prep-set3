package com.interviewprep.library.repository;

import com.interviewprep.library.entity.Book;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Book b set b.borrowed = true where b.id = :id and b.borrowed = false")
    int markBorrowedIfAvailable(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Book b set b.borrowed = false where b.id = :id and b.borrowed = true")
    int markReturnedIfBorrowed(@Param("id") UUID id);
}
