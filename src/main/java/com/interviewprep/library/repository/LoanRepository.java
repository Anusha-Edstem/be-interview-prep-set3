package com.interviewprep.library.repository;

import com.interviewprep.library.entity.Loan;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Optional<Loan> findByBookIdAndReturnedAtIsNull(UUID bookId);
}
