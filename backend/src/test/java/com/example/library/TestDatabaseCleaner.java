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
        jdbcTemplate.execute("SET session_replication_role = 'replica'");
        jdbcTemplate.execute("TRUNCATE TABLE loans");
        jdbcTemplate.execute("TRUNCATE TABLE books RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE members RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("SET session_replication_role = 'origin'");
    }
}
