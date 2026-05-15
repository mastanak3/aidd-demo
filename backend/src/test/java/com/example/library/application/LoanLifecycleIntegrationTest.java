package com.example.library.application;

import com.example.library.TestDatabaseCleaner;
import com.example.library.domain.model.Book;
import com.example.library.domain.model.Loan;
import com.example.library.domain.model.Member;
import com.example.library.domain.model.MemberType;
import com.example.library.infrastructure.database.DataSourceProducer;
import com.example.library.infrastructure.repository.JdbcBookRepository;
import com.example.library.infrastructure.repository.JdbcLoanRepository;
import com.example.library.infrastructure.repository.JdbcMemberRepository;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@EnableWeld
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanLifecycleIntegrationTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.of(
        LoanService.class,
        BookService.class,
        MemberService.class,
        JdbcBookRepository.class,
        JdbcMemberRepository.class,
        JdbcLoanRepository.class,
        DataSourceProducer.class,
        TestDatabaseCleaner.class
    );

    @Inject
    LoanService loanService;

    @Inject
    BookService bookService;

    @Inject
    MemberService memberService;

    @Inject
    TestDatabaseCleaner dbCleaner;

    private static Long memberId;
    private static Long bookId;
    private static Long loanId;

    @BeforeEach
    void setUp() {
        if (memberId == null) {
            dbCleaner.cleanAll();
            Member member = memberService.create("統合テスト会員", "lifecycle@example.com", MemberType.GENERAL);
            memberId = member.getId();
            Book book = bookService.create("統合テスト書籍", "テスト著者", "ISBN-LIFECYCLE");
            bookId = book.getId();
        }
    }

    @Test
    @Order(1)
    void 書籍を貸出できる() {
        Loan loan = loanService.borrowBook(memberId, bookId);

        assertNotNull(loan.getId());
        assertEquals(memberId, loan.getMemberId());
        assertEquals(bookId, loan.getBookId());
        assertTrue(loan.isActive());

        loanId = loan.getId();
    }

    @Test
    @Order(2)
    void 貸出中の書籍は利用不可になる() {
        Book found = bookService.findById(bookId);

        assertFalse(found.isAvailable());
    }

    @Test
    @Order(3)
    void 書籍を返却できる() {
        Loan returned = loanService.returnBook(loanId);

        assertNotNull(returned.getReturnDate());
        assertFalse(returned.isActive());
    }

    @Test
    @Order(4)
    void 返却後は書籍が利用可能になる() {
        Book found = bookService.findById(bookId);

        assertTrue(found.isAvailable());
    }
}
