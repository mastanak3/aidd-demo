package com.example.library.resource;

import com.example.library.App;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanResourceTest {

    private static final int PORT = 18083;
    private static HttpServer server;
    private static Weld weld;
    private static int memberId;
    private static int bookId;

    @BeforeAll
    static void setUp() {
        weld = new Weld();
        WeldContainer container = weld.initialize();
        server = App.startServer(container, PORT);
        RestAssured.baseURI = "http://localhost:" + PORT + "/api/";

        memberId = given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "田中太郎", "email": "tanaka@example.com", "memberType": "GENERAL"}
                """)
        .when()
            .post("members")
        .then()
            .extract().path("id");

        bookId = given()
            .contentType(ContentType.JSON)
            .body("""
                {"title": "テスト駆動開発", "author": "Kent Beck", "isbn": "978-4-274-21788-0"}
                """)
        .when()
            .post("books")
        .then()
            .extract().path("id");
    }

    @AfterAll
    static void tearDown() {
        if (server != null) server.shutdown();
        if (weld != null) weld.shutdown();
    }

    @Test
    @Order(1)
    void 書籍を貸出できる() {
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
    @Order(2)
    void 全貸出を取得できる() {
        when()
            .get("loans")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(3)
    void 書籍を返却できる() {
        int loanId = when()
            .get("loans")
        .then()
            .extract().path("[0].id");

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
    @Order(4)
    void 貸出中の書籍は借りられない() {
        int newBookId = given()
            .contentType(ContentType.JSON)
            .body("""
                {"title": "書籍X", "author": "著者X", "isbn": "ISBN-X"}
                """)
        .when()
            .post("books")
        .then()
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": %d, "bookId": %d}
                """.formatted(memberId, newBookId))
        .when()
            .post("loans")
        .then()
            .statusCode(201);

        int anotherMemberId = given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "鈴木", "email": "suzuki@example.com", "memberType": "GENERAL"}
                """)
        .when()
            .post("members")
        .then()
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": %d, "bookId": %d}
                """.formatted(anotherMemberId, newBookId))
        .when()
            .post("loans")
        .then()
            .statusCode(409);
    }
}
