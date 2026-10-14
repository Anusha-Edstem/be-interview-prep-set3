package com.interviewprep.library.mapper;

import com.interviewprep.library.dto.LoanResponse;
import com.interviewprep.library.entity.Loan;
import org.springframework.stereotype.Component;

@Component
public class LoanMapper {

    public LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(), loan.getBook().getId(), loan.getMemberId(), loan.getBorrowedAt(), loan.getReturnedAt());
    }
}
