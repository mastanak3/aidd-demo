package com.example.library;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestDatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void cleanAll() {
        jdbcTemplate.execute("TRUNCATE TABLE loans, books, members RESTART IDENTITY CASCADE");
    }
}
