package com.example.library.application;

import com.example.library.domain.LendingPolicy;
import com.example.library.domain.model.Book;
import com.example.library.domain.model.Loan;
import com.example.library.domain.model.Member;
import com.example.library.domain.repository.BookRepository;
import com.example.library.domain.repository.LoanRepository;
import com.example.library.domain.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LoanService {

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;

    public LoanService(BookRepository bookRepository, MemberRepository memberRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
    }

    public Loan borrowBook(Long memberId, Long bookId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません: ID=" + memberId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: ID=" + bookId));

        if (!book.isAvailable()) {
            throw new IllegalStateException("この書籍は現在貸出中です");
        }

        List<Loan> activeLoans = loanRepository.findByMemberIdAndReturnDateIsNull(memberId);
        if (!LendingPolicy.canBorrow(member.getMemberType(), activeLoans.size())) {
            throw new IllegalStateException("貸出上限に達しています（上限: "
                    + LendingPolicy.getMaxLoans(member.getMemberType()) + "冊）");
        }

        LocalDate loanDate = LocalDate.now();
        LocalDate dueDate = LendingPolicy.calculateDueDate(member.getMemberType(), loanDate);

        book.checkout();
        bookRepository.save(book);

        Loan loan = new Loan(bookId, memberId, loanDate, dueDate);
        return loanRepository.save(loan);
    }

    public Loan returnBook(Long loanId) {
        return returnBook(loanId, false);
    }

    public Loan returnBook(Long loanId, boolean isBookPostReturn) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("貸出記録が見つかりません: ID=" + loanId));

        Book book = bookRepository.findById(loan.getBookId())
                .orElseThrow(() -> new IllegalStateException("書籍が見つかりません: ID=" + loan.getBookId()));

        Member member = memberRepository.findById(loan.getMemberId())
                .orElseThrow(() -> new IllegalStateException("会員が見つかりません: ID=" + loan.getMemberId()));

        LocalDate returnDate = LocalDate.now();
        if (isBookPostReturn) {
            returnDate = returnDate.minusDays(1);
        }

        loan.returnBook(returnDate);
        book.returnBook();

        int overdueFee = LendingPolicy.calculateOverdueFee(member.getMemberType(), loan.getDueDate(), returnDate);
        loan.setOverdueFee(overdueFee);

        bookRepository.save(book);
        return loanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public List<Loan> findAll() {
        return loanRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Loan findById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("貸出記録が見つかりません: ID=" + id));
    }
}
