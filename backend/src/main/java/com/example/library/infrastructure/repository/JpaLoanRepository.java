package com.example.library.infrastructure.repository;

import com.example.library.domain.model.Loan;
import com.example.library.domain.repository.LoanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JpaLoanRepository implements LoanRepository {

    @Inject
    private EntityManager em;

    @Override
    public Loan save(Loan loan) {
        if (loan.getId() == null) {
            em.persist(loan);
            return loan;
        }
        return em.merge(loan);
    }

    @Override
    public Optional<Loan> findById(Long id) {
        return Optional.ofNullable(em.find(Loan.class, id));
    }

    @Override
    public List<Loan> findAll() {
        return em.createQuery("SELECT l FROM Loan l ORDER BY l.id", Loan.class).getResultList();
    }

    @Override
    public List<Loan> findActiveByMemberId(Long memberId) {
        return em.createQuery(
                "SELECT l FROM Loan l WHERE l.memberId = :memberId AND l.returnDate IS NULL", Loan.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }
}
