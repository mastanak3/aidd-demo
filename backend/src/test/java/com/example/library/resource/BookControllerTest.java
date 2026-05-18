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
class BookControllerTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanAll();
    }

    @Test
    void 書籍を登録してレスポンスを検証() {
        var response = restTemplate.postForEntity("/api/books",
                Map.of("title", "テスト駆動開発", "author", "Kent Beck", "isbn", "978-4-274-21788-0"),
                Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("テスト駆動開発", response.getBody().get("title"));
        assertEquals("Kent Beck", response.getBody().get("author"));
        assertEquals(true, response.getBody().get("available"));
        assertNotNull(response.getBody().get("id"));
    }

    @Test
    void 全書籍を取得できる() {
        restTemplate.postForEntity("/api/books",
                Map.of("title", "書籍A", "author", "著者A", "isbn", "ISBN-A"), Map.class);

        var response = restTemplate.getForEntity("/api/books", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().size() >= 1);
    }

    @Test
    void IDで書籍を取得できる() {
        var created = restTemplate.postForEntity("/api/books",
                Map.of("title", "リファクタリング", "author", "Martin Fowler", "isbn", "978-4-274-22454-3"),
                Map.class);
        var id = created.getBody().get("id");

        var response = restTemplate.getForEntity("/api/books/" + id, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("リファクタリング", response.getBody().get("title"));
    }

    @Test
    void 書籍を更新できる() {
        var created = restTemplate.postForEntity("/api/books",
                Map.of("title", "旧タイトル", "author", "旧著者", "isbn", "OLD"), Map.class);
        var id = created.getBody().get("id");

        var request = new HttpEntity<>(Map.of("title", "新タイトル", "author", "新著者", "isbn", "NEW"));
        var response = restTemplate.exchange("/api/books/" + id, HttpMethod.PUT, request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("新タイトル", response.getBody().get("title"));
        assertEquals("新著者", response.getBody().get("author"));
    }

    @Test
    void 書籍を削除できる() {
        var created = restTemplate.postForEntity("/api/books",
                Map.of("title", "削除対象", "author", "著者", "isbn", "DEL"), Map.class);
        var id = created.getBody().get("id");

        var response = restTemplate.exchange("/api/books/" + id, HttpMethod.DELETE, null, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
