package com.example.library.infrastructure.repository;

import com.example.library.domain.model.Book;
import com.example.library.domain.repository.BookRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class InMemoryBookRepository implements BookRepository {

    private final Map<Long, Book> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Book save(Book book) {
        if (book.getId() == null) {
            book.setId(sequence.incrementAndGet());
        }
        store.put(book.getId(), book);
        return book;
    }

    @Override
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
