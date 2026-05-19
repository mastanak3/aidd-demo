package com.example.library.application;

import com.example.library.domain.model.Book;
import com.example.library.domain.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book create(String title, String author, String isbn) {
        return create(title, author, isbn, false);
    }

    public Book create(String title, String author, String isbn, boolean newRelease) {
        Book book = new Book(title, author, isbn);
        book.setNewRelease(newRelease);
        return bookRepository.save(book);
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: ID=" + id));
    }

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book update(Long id, String title, String author, String isbn) {
        return update(id, title, author, isbn, false);
    }

    public Book update(Long id, String title, String author, String isbn, boolean newRelease) {
        Book book = findById(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setNewRelease(newRelease);
        return bookRepository.save(book);
    }

    public void delete(Long id) {
        findById(id);
        bookRepository.deleteById(id);
    }
}
