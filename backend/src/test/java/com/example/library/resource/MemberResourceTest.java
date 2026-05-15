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
class MemberResourceTest {

    private static final int PORT = 18082;
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
    void 一般会員を登録できる() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "田中太郎", "email": "tanaka@example.com", "memberType": "GENERAL"}
                """)
        .when()
            .post("members")
        .then()
            .statusCode(201)
            .body("name", equalTo("田中太郎"))
            .body("email", equalTo("tanaka@example.com"))
            .body("memberType", equalTo("GENERAL"))
            .body("id", notNullValue());
    }

    @Test
    void プレミアム会員を登録できる() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "鈴木花子", "email": "suzuki@example.com", "memberType": "PREMIUM"}
                """)
        .when()
            .post("members")
        .then()
            .statusCode(201)
            .body("memberType", equalTo("PREMIUM"));
    }

    @Test
    void 全会員を取得できる() {
        // テストデータを作成
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "会員A", "email": "a@example.com", "memberType": "GENERAL"}
                """)
        .when()
            .post("members");

        when()
            .get("members")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void IDで会員を取得できる() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "佐藤次郎", "email": "sato@example.com", "memberType": "GENERAL"}
                """)
        .when()
            .post("members")
        .then()
            .extract().path("id");

        when()
            .get("members/" + id)
        .then()
            .statusCode(200)
            .body("name", equalTo("佐藤次郎"));
    }

    @Test
    void 会員を更新できる() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "旧名前", "email": "old@example.com", "memberType": "GENERAL"}
                """)
        .when()
            .post("members")
        .then()
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "新名前", "email": "new@example.com", "memberType": "PREMIUM"}
                """)
        .when()
            .put("members/" + id)
        .then()
            .statusCode(200)
            .body("name", equalTo("新名前"))
            .body("memberType", equalTo("PREMIUM"));
    }

    @Test
    void 会員を削除できる() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "削除対象", "email": "del@example.com", "memberType": "GENERAL"}
                """)
        .when()
            .post("members")
        .then()
            .extract().path("id");

        when()
            .delete("members/" + id)
        .then()
            .statusCode(204);
    }
}
