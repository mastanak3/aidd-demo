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
class MemberControllerTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanAll();
    }

    @Test
    void 一般会員を登録できる() {
        var response = restTemplate.postForEntity("/api/members",
                Map.of("name", "田中太郎", "email", "tanaka@example.com", "memberType", "GENERAL"),
                Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("田中太郎", response.getBody().get("name"));
        assertEquals("tanaka@example.com", response.getBody().get("email"));
        assertEquals("GENERAL", response.getBody().get("memberType"));
        assertNotNull(response.getBody().get("id"));
    }

    @Test
    void プレミアム会員を登録できる() {
        var response = restTemplate.postForEntity("/api/members",
                Map.of("name", "鈴木花子", "email", "suzuki@example.com", "memberType", "PREMIUM"),
                Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("PREMIUM", response.getBody().get("memberType"));
    }

    @Test
    void 全会員を取得できる() {
        restTemplate.postForEntity("/api/members",
                Map.of("name", "会員A", "email", "a@example.com", "memberType", "GENERAL"), Map.class);

        var response = restTemplate.getForEntity("/api/members", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().size() >= 1);
    }

    @Test
    void IDで会員を取得できる() {
        var created = restTemplate.postForEntity("/api/members",
                Map.of("name", "佐藤次郎", "email", "sato@example.com", "memberType", "GENERAL"),
                Map.class);
        var id = created.getBody().get("id");

        var response = restTemplate.getForEntity("/api/members/" + id, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("佐藤次郎", response.getBody().get("name"));
    }

    @Test
    void 会員を更新できる() {
        var created = restTemplate.postForEntity("/api/members",
                Map.of("name", "旧名前", "email", "old@example.com", "memberType", "GENERAL"),
                Map.class);
        var id = created.getBody().get("id");

        var request = new HttpEntity<>(Map.of("name", "新名前", "email", "new@example.com", "memberType", "PREMIUM"));
        var response = restTemplate.exchange("/api/members/" + id, HttpMethod.PUT, request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("新名前", response.getBody().get("name"));
        assertEquals("PREMIUM", response.getBody().get("memberType"));
    }

    @Test
    void 会員を削除できる() {
        var created = restTemplate.postForEntity("/api/members",
                Map.of("name", "削除対象", "email", "del@example.com", "memberType", "GENERAL"),
                Map.class);
        var id = created.getBody().get("id");

        var response = restTemplate.exchange("/api/members/" + id, HttpMethod.DELETE, null, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
