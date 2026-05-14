package com.example.library.resource;

import com.example.library.App;
import com.example.library.TestDatabaseCleaner;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

class LoanResourceTest {

    private static final int PORT = 18083;
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
    void 書籍を貸出できる() {
        int memberId = createMember("田中太郎", "tanaka@example.com");
        int bookId = createBook("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": %d, "bookId": %d}
                """.formatted(memberId, bookId))
        .when()
            .post("loans")
        .then()
            .statusCode(201)
            .body("bookId", equalTo(bookId))
            .body("memberId", equalTo(memberId))
            .body("loanDate", notNullValue())
            .body("dueDate", notNullValue())
            .body("returnDate", nullValue())
            .body("active", equalTo(true));
    }

    @Test
    void 全貸出を取得できる() {
        int memberId = createMember("田中太郎", "tanaka@example.com");
        int bookId = createBook("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        // 貸出を作成
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": %d, "bookId": %d}
                """.formatted(memberId, bookId))
        .when()
            .post("loans");

        when()
            .get("loans")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void 書籍を返却できる() {
        int memberId = createMember("田中太郎", "tanaka@example.com");
        int bookId = createBook("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        // 貸出を作成
        int loanId = given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": %d, "bookId": %d}
                """.formatted(memberId, bookId))
        .when()
            .post("loans")
        .then()
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
        .when()
            .post("loans/" + loanId + "/return")
        .then()
            .statusCode(200)
            .body("returnDate", notNullValue())
            .body("active", equalTo(false));
    }

    @Test
    void 貸出中の書籍は借りられない() {
        int memberId = createMember("田中太郎", "tanaka@example.com");
        int bookId = createBook("書籍X", "著者X", "ISBN-X");

        // 1人目が借りる
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": %d, "bookId": %d}
                """.formatted(memberId, bookId))
        .when()
            .post("loans")
        .then()
            .statusCode(201);

        // 2人目が同じ本を借りようとする
        int anotherMemberId = createMember("鈴木", "suzuki@example.com");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": %d, "bookId": %d}
                """.formatted(anotherMemberId, bookId))
        .when()
            .post("loans")
        .then()
            .statusCode(409);
    }

    private int createMember(String name, String email) {
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "%s", "email": "%s", "memberType": "GENERAL"}
                """.formatted(name, email))
        .when()
            .post("members")
        .then()
            .extract().path("id");
    }

    private int createBook(String title, String author, String isbn) {
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {"title": "%s", "author": "%s", "isbn": "%s"}
                """.formatted(title, author, isbn))
        .when()
            .post("books")
        .then()
            .extract().path("id");
    }
}
