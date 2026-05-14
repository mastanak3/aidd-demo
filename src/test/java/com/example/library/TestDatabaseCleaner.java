package com.example.library;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * テスト用のデータベースクリーンアップユーティリティ
 */
@ApplicationScoped
public class TestDatabaseCleaner {

    @Inject
    private DataSource dataSource;

    /**
     * すべてのテーブルのデータをクリアする
     */
    public void cleanAll() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // 外部キー制約を一時的に無効化
            stmt.execute("SET session_replication_role = 'replica'");

            // すべてのテーブルをTRUNCATE
            stmt.execute("TRUNCATE TABLE loans");
            stmt.execute("TRUNCATE TABLE books RESTART IDENTITY CASCADE");
            stmt.execute("TRUNCATE TABLE members RESTART IDENTITY CASCADE");

            // 外部キー制約を再度有効化
            stmt.execute("SET session_replication_role = 'origin'");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clean database", e);
        }
    }
}
