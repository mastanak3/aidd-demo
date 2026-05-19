package com.example.library.domain.repository;

import com.example.library.domain.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByMemberIdAndReturnDateIsNull(String memberId);
}
