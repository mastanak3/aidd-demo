package com.example.library.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookTest {

    @Test
    void 新規書籍は貸出可能な状態で生成される() {
        Book book = new Book("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        assertEquals("テスト駆動開発", book.getTitle());
        assertEquals("Kent Beck", book.getAuthor());
        assertEquals("978-4-274-21788-0", book.getIsbn());
        assertTrue(book.isAvailable());
    }

    @Test
    void 貸出すると貸出不可になる() {
        Book book = new Book("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        book.checkout();

        assertFalse(book.isAvailable());
    }

    @Test
    void 返却すると貸出可能に戻る() {
        Book book = new Book("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");
        book.checkout();

        book.returnBook();

        assertTrue(book.isAvailable());
    }

    @Test
    void 貸出中の書籍を二重に貸出すると例外() {
        Book book = new Book("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");
        book.checkout();

        assertThrows(IllegalStateException.class, book::checkout);
    }

    @Test
    void 貸出中でない書籍を返却すると例外() {
        Book book = new Book("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        assertThrows(IllegalStateException.class, book::returnBook);
    }
}
