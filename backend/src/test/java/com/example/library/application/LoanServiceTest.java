package com.example.library.application;

import com.example.library.TestDatabaseCleaner;
import com.example.library.domain.model.Book;
import com.example.library.domain.model.Loan;
import com.example.library.domain.model.Member;
import com.example.library.domain.model.MemberType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoanServiceTest {

    @Autowired
    LoanService loanService;

    @Autowired
    BookService bookService;

    @Autowired
    MemberService memberService;

    @Autowired
    TestDatabaseCleaner dbCleaner;

    private Member generalMember;
    private Book book;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();
        generalMember = memberService.create("田中太郎", "tanaka@example.com", MemberType.GENERAL);
        book = bookService.create("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");
    }

    @Test
    void 書籍を貸出できる() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());

        assertNotNull(loan.getId());
        assertEquals(book.getId(), loan.getBookId());
        assertEquals(generalMember.getId(), loan.getMemberId());
        assertNotNull(loan.getLoanDate());
        assertNotNull(loan.getDueDate());
        assertNull(loan.getReturnDate());
        assertTrue(loan.isActive());
    }

    @Test
    void 貸出すると書籍が貸出不可になる() {
        loanService.borrowBook(generalMember.getId(), book.getId());

        Book found = bookService.findById(book.getId());
        assertFalse(found.isAvailable());
    }

    @Test
    void 貸出中の書籍は借りられない() {
        loanService.borrowBook(generalMember.getId(), book.getId());

        Member anotherMember = memberService.create("鈴木", "suzuki@example.com", MemberType.GENERAL);
        assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(anotherMember.getId(), book.getId()));
    }

    @Test
    void 一般会員は3冊まで借りられる() {
        Book book2 = bookService.create("書籍2", "著者2", "ISBN-2");
        Book book3 = bookService.create("書籍3", "著者3", "ISBN-3");

        loanService.borrowBook(generalMember.getId(), book.getId());
        loanService.borrowBook(generalMember.getId(), book2.getId());
        loanService.borrowBook(generalMember.getId(), book3.getId());

        assertEquals(3, loanService.findAll().stream()
                .filter(Loan::isActive)
                .filter(l -> l.getMemberId().equals(generalMember.getId()))
                .count());
    }

    @Test
    void 一般会員は4冊目を借りると例外() {
        Book book2 = bookService.create("書籍2", "著者2", "ISBN-2");
        Book book3 = bookService.create("書籍3", "著者3", "ISBN-3");
        Book book4 = bookService.create("書籍4", "著者4", "ISBN-4");

        loanService.borrowBook(generalMember.getId(), book.getId());
        loanService.borrowBook(generalMember.getId(), book2.getId());
        loanService.borrowBook(generalMember.getId(), book3.getId());

        assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(generalMember.getId(), book4.getId()));
    }

    @Test
    void プレミアム会員は10冊まで借りられる() {
        Member premiumMember = memberService.create("高橋", "takahashi@example.com", MemberType.PREMIUM);

        for (int i = 0; i < 10; i++) {
            Book b = bookService.create("書籍" + i, "著者" + i, "ISBN-" + i);
            loanService.borrowBook(premiumMember.getId(), b.getId());
        }

        assertEquals(10, loanService.findAll().stream()
                .filter(Loan::isActive)
                .filter(l -> l.getMemberId().equals(premiumMember.getId()))
                .count());
    }

    @Test
    void 書籍を返却できる() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());

        Loan returned = loanService.returnBook(loan.getId());

        assertNotNull(returned.getReturnDate());
        assertFalse(returned.isActive());
    }

    @Test
    void 返却すると書籍が貸出可能に戻る() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());

        loanService.returnBook(loan.getId());

        Book found = bookService.findById(book.getId());
        assertTrue(found.isAvailable());
    }

    @Test
    void 返却後は再度借りられる() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());
        loanService.returnBook(loan.getId());

        Loan newLoan = loanService.borrowBook(generalMember.getId(), book.getId());

        assertNotNull(newLoan.getId());
        assertTrue(newLoan.isActive());
    }

    @Test
    void 存在しない会員で貸出すると例外() {
        assertThrows(IllegalArgumentException.class,
                () -> loanService.borrowBook("9999999", book.getId()));
    }

    @Test
    void 存在しない書籍を貸出すると例外() {
        assertThrows(IllegalArgumentException.class,
                () -> loanService.borrowBook(generalMember.getId(), 9999L));
    }
}
