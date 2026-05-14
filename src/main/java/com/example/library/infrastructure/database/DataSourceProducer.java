package com.example.library.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@ApplicationScoped
public class DataSourceProducer {

    private HikariDataSource dataSource;

    @PostConstruct
    void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(envOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/library"));
        config.setUsername(envOrDefault("DATABASE_USER", "library"));
        config.setPassword(envOrDefault("DATABASE_PASSWORD", "library"));
        config.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(config);
        initSchema();
    }

    @Produces
    @ApplicationScoped
    public DataSource dataSource() {
        return dataSource;
    }

    @PreDestroy
    void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void initSchema() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new RuntimeException("schema.sql not found on classpath");
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
