package com.example.library.domain.repository;

import com.example.library.domain.model.Loan;

import java.util.List;
import java.util.Optional;

public interface LoanRepository {

    Loan save(Loan loan);

    Optional<Loan> findById(Long id);

    List<Loan> findAll();

    List<Loan> findActiveByMemberId(Long memberId);
}
