package com.example.library.resource;

import com.example.library.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoanControllerTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanAll();
    }

    @Test
    void 書籍を貸出できる() {
        String memberId = createMember("0000001", "田中太郎", "tanaka@example.com");
        int bookId = createBook("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        var response = restTemplate.postForEntity("/api/loans",
                Map.of("memberId", memberId, "bookId", bookId), Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(bookId, response.getBody().get("bookId"));
        assertEquals(memberId, response.getBody().get("memberId"));
        assertNotNull(response.getBody().get("loanDate"));
        assertNotNull(response.getBody().get("dueDate"));
        assertNull(response.getBody().get("returnDate"));
        assertEquals(true, response.getBody().get("active"));
    }

    @Test
    void 全貸出を取得できる() {
        String memberId = createMember("0000001", "田中太郎", "tanaka@example.com");
        int bookId = createBook("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        restTemplate.postForEntity("/api/loans",
                Map.of("memberId", memberId, "bookId", bookId), Map.class);

        var response = restTemplate.getForEntity("/api/loans", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().size() >= 1);
    }

    @Test
    void 書籍を返却できる() {
        String memberId = createMember("0000001", "田中太郎", "tanaka@example.com");
        int bookId = createBook("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        var loanResponse = restTemplate.postForEntity("/api/loans",
                Map.of("memberId", memberId, "bookId", bookId), Map.class);
        int loanId = (int) loanResponse.getBody().get("id");

        var response = restTemplate.postForEntity("/api/loans/" + loanId + "/return", null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().get("returnDate"));
        assertEquals(false, response.getBody().get("active"));
    }

    @Test
    void 貸出中の書籍は借りられない() {
        String memberId = createMember("0000001", "田中太郎", "tanaka@example.com");
        int bookId = createBook("書籍X", "著者X", "ISBN-X");

        restTemplate.postForEntity("/api/loans",
                Map.of("memberId", memberId, "bookId", bookId), Map.class);

        String anotherMemberId = createMember("0000002", "鈴木", "suzuki@example.com");
        var response = restTemplate.postForEntity("/api/loans",
                Map.of("memberId", anotherMemberId, "bookId", bookId), Map.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    private String createMember(String id, String name, String email) {
        var response = restTemplate.postForEntity("/api/members",
                Map.of("id", id, "name", name, "email", email, "memberType", "GENERAL"), Map.class);
        return (String) response.getBody().get("id");
    }

    private int createBook(String title, String author, String isbn) {
        var response = restTemplate.postForEntity("/api/books",
                Map.of("title", title, "author", author, "isbn", isbn), Map.class);
        return (int) response.getBody().get("id");
    }
}
