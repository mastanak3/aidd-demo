package com.example.library.resource;

import com.example.library.App;
import com.example.library.TestDatabaseCleaner;
import org.junit.jupiter.api.Tag;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@Tag("e2e")
class BookResourceTest {

    private static final int PORT = 18081;
    private static HttpServer server;
    private static Weld weld;
    private static WeldContainer container;
    private static TestDatabaseCleaner cleaner;

    @BeforeAll
    static void setUpClass() {
        weld = new Weld();
        container = weld.initialize();
        cleaner = container.select(TestDatabaseCleaner.class).get();
        server = App.startServer(container, PORT);
        RestAssured.baseURI = "http://localhost:" + PORT + "/api/";
    }

    @BeforeEach
    void setUp() {
        cleaner.cleanAll();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) server.shutdown();
        if (weld != null) weld.shutdown();
    }

    @Test
    void 書籍を登録してレスポンスを検証() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"title": "テスト駆動開発", "author": "Kent Beck", "isbn": "978-4-274-21788-0"}
                """)
        .when()
            .post("books")
        .then()
            .statusCode(201)
            .body("title", equalTo("テスト駆動開発"))
            .body("author", equalTo("Kent Beck"))
            .body("isbn", equalTo("978-4-274-21788-0"))
            .body("available", equalTo(true))
            .body("id", notNullValue());
    }

    @Test
    void 全書籍を取得できる() {
        // テストデータを作成
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"title": "書籍A", "author": "著者A", "isbn": "ISBN-A"}
                """)
        .when()
            .post("books");

        when()
            .get("books")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void IDで書籍を取得できる() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"title": "リファクタリング", "author": "Martin Fowler", "isbn": "978-4-274-22454-3"}
                """)
        .when()
            .post("books")
        .then()
            .extract().path("id");

        when()
            .get("books/" + id)
        .then()
            .statusCode(200)
            .body("title", equalTo("リファクタリング"));
    }

    @Test
    void 書籍を更新できる() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"title": "旧タイトル", "author": "旧著者", "isbn": "OLD"}
                """)
        .when()
            .post("books")
        .then()
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"title": "新タイトル", "author": "新著者", "isbn": "NEW"}
                """)
        .when()
            .put("books/" + id)
        .then()
            .statusCode(200)
            .body("title", equalTo("新タイトル"))
            .body("author", equalTo("新著者"));
    }

    @Test
    void 書籍を削除できる() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"title": "削除対象", "author": "著者", "isbn": "DEL"}
                """)
        .when()
            .post("books")
        .then()
            .extract().path("id");

        when()
            .delete("books/" + id)
        .then()
            .statusCode(204);
    }
}
