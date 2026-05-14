package com.example.library.infrastructure.repository;

import com.example.library.domain.model.Loan;
import com.example.library.domain.repository.LoanRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class InMemoryLoanRepository implements LoanRepository {

    private final Map<Long, Loan> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Loan save(Loan loan) {
        if (loan.getId() == null) {
            loan.setId(sequence.incrementAndGet());
        }
        store.put(loan.getId(), loan);
        return loan;
    }

    @Override
    public Optional<Loan> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Loan> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Loan> findActiveByMemberId(Long memberId) {
        return store.values().stream()
                .filter(loan -> loan.getMemberId().equals(memberId))
                .filter(Loan::isActive)
                .toList();
    }
}
