package com.example.library.application;

import com.example.library.TestDatabaseCleaner;
import com.example.library.TestEntityManagerProducer;
import com.example.library.TestTransactionInterceptor;
import com.example.library.domain.model.Book;
import com.example.library.infrastructure.repository.JpaBookRepository;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@EnableWeld
class BookServiceTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.of(
        BookService.class,
        JpaBookRepository.class,
        TestEntityManagerProducer.class,
        TestTransactionInterceptor.class,
        TestDatabaseCleaner.class
    );

    @Inject
    BookService bookService;

    @Inject
    TestDatabaseCleaner dbCleaner;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();
    }

    @Test
    void 書籍を登録できる() {
        Book book = bookService.create("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        assertNotNull(book.getId());
        assertEquals("テスト駆動開発", book.getTitle());
        assertEquals("Kent Beck", book.getAuthor());
        assertEquals("978-4-274-21788-0", book.getIsbn());
        assertTrue(book.isAvailable());
    }

    @Test
    void 書籍をIDで検索できる() {
        Book created = bookService.create("リファクタリング", "Martin Fowler", "978-4-274-22454-3");

        Book found = bookService.findById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("リファクタリング", found.getTitle());
    }

    @Test
    void 全書籍を取得できる() {
        bookService.create("書籍A", "著者A", "ISBN-A");
        bookService.create("書籍B", "著者B", "ISBN-B");

        var books = bookService.findAll();

        assertTrue(books.size() >= 2);
    }

    @Test
    void 書籍を更新できる() {
        Book created = bookService.create("旧タイトル", "旧著者", "ISBN-OLD");

        Book updated = bookService.update(created.getId(), "新タイトル", "新著者", "ISBN-NEW");

        assertEquals("新タイトル", updated.getTitle());
        assertEquals("新著者", updated.getAuthor());
        assertEquals("ISBN-NEW", updated.getIsbn());
    }

    @Test
    void 書籍を削除できる() {
        Book created = bookService.create("削除対象", "著者", "ISBN-DEL");
        Long id = created.getId();

        bookService.delete(id);

        assertThrows(IllegalArgumentException.class, () -> bookService.findById(id));
    }

    @Test
    void 存在しないIDで検索すると例外() {
        assertThrows(IllegalArgumentException.class, () -> bookService.findById(9999L));
    }
}
