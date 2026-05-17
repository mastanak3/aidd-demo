package com.example.library.infrastructure.repository;

import com.example.library.domain.model.Loan;
import com.example.library.domain.repository.LoanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcLoanRepository implements LoanRepository {

    @Inject
    private DataSource dataSource;

    @Override
    public Loan save(Loan loan) {
        if (loan.getId() == null) {
            return insert(loan);
        }
        return update(loan);
    }

    @Override
    public Optional<Loan> findById(Long id) {
        String sql = "SELECT id, book_id, member_id, loan_date, due_date, return_date, overdue_fee FROM loans WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Loan> findAll() {
        String sql = "SELECT id, book_id, member_id, loan_date, due_date, return_date, overdue_fee FROM loans ORDER BY id";
        List<Loan> loans = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                loans.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return loans;
    }

    @Override
    public List<Loan> findActiveByMemberId(Long memberId) {
        String sql = "SELECT id, book_id, member_id, loan_date, due_date, return_date, overdue_fee FROM loans WHERE member_id = ? AND return_date IS NULL";
        List<Loan> loans = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return loans;
    }

    private Loan insert(Loan loan) {
        String sql = "INSERT INTO loans (book_id, member_id, loan_date, due_date, return_date, overdue_fee) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, loan.getBookId());
            ps.setLong(2, loan.getMemberId());
            ps.setDate(3, Date.valueOf(loan.getLoanDate()));
            ps.setDate(4, Date.valueOf(loan.getDueDate()));
            if (loan.getReturnDate() != null) {
                ps.setDate(5, Date.valueOf(loan.getReturnDate()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setInt(6, loan.getOverdueFee());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                loan.setId(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return loan;
    }

    private Loan update(Loan loan) {
        String sql = "UPDATE loans SET book_id = ?, member_id = ?, loan_date = ?, due_date = ?, return_date = ?, overdue_fee = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, loan.getBookId());
            ps.setLong(2, loan.getMemberId());
            ps.setDate(3, Date.valueOf(loan.getLoanDate()));
            ps.setDate(4, Date.valueOf(loan.getDueDate()));
            if (loan.getReturnDate() != null) {
                ps.setDate(5, Date.valueOf(loan.getReturnDate()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setInt(6, loan.getOverdueFee());
            ps.setLong(7, loan.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return loan;
    }

    private Loan mapRow(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getLong("id"));
        loan.setBookId(rs.getLong("book_id"));
        loan.setMemberId(rs.getLong("member_id"));
        loan.setLoanDate(rs.getDate("loan_date").toLocalDate());
        loan.setDueDate(rs.getDate("due_date").toLocalDate());
        Date returnDate = rs.getDate("return_date");
        if (returnDate != null) {
            loan.setReturnDate(returnDate.toLocalDate());
        }
        loan.setOverdueFee(rs.getInt("overdue_fee"));
        return loan;
    }
}
