package com.example.library.application;

import com.example.library.domain.model.Book;
import com.example.library.domain.repository.BookRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class BookService {

    @Inject
    private BookRepository bookRepository;

    public Book create(String title, String author, String isbn) {
        Book book = new Book(title, author, isbn);
        return bookRepository.save(book);
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: ID=" + id));
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book update(Long id, String title, String author, String isbn) {
        Book book = findById(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        return bookRepository.save(book);
    }

    public void delete(Long id) {
        findById(id);
        bookRepository.deleteById(id);
    }
}
