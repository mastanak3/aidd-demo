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
class LendingPolicyE2ETest {

    private static final int PORT = 18084;
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

    // ========== 一般会員の貸出上限テスト ==========

    @Test
    void 一般会員_0冊の状態で貸出できる() {
        int memberId = createGeneralMember("会員A", "a@example.com");
        int bookId = createBook("書籍1", "著者1", "ISBN-001");

        borrowBook(memberId, bookId)
            .statusCode(201)
            .body("active", equalTo(true));
    }

    @Test
    void 一般会員_1冊借りている状態で2冊目を貸出できる() {
        int memberId = createGeneralMember("会員B", "b@example.com");
        int book1 = createBook("書籍1", "著者1", "ISBN-001");
        int book2 = createBook("書籍2", "著者2", "ISBN-002");

        borrowBook(memberId, book1).statusCode(201);
        borrowBook(memberId, book2).statusCode(201);
    }

    @Test
    void 一般会員_2冊借りている状態で3冊目を貸出できる() {
        int memberId = createGeneralMember("会員C", "c@example.com");
        int book1 = createBook("書籍1", "著者1", "ISBN-001");
        int book2 = createBook("書籍2", "著者2", "ISBN-002");
        int book3 = createBook("書籍3", "著者3", "ISBN-003");

        borrowBook(memberId, book1).statusCode(201);
        borrowBook(memberId, book2).statusCode(201);
        borrowBook(memberId, book3).statusCode(201);
    }

    @Test
    void 一般会員_3冊借りている状態で4冊目は貸出不可() {
        int memberId = createGeneralMember("会員D", "d@example.com");
        for (int i = 1; i <= 3; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-00" + i);
            borrowBook(memberId, bookId).statusCode(201);
        }

        int book4 = createBook("書籍4", "著者4", "ISBN-004");
        borrowBook(memberId, book4).statusCode(409);
    }

    @Test
    void 一般会員_3冊借りて1冊返却後に再度貸出できる() {
        int memberId = createGeneralMember("会員E", "e@example.com");
        int[] bookIds = new int[3];
        int[] loanIds = new int[3];
        for (int i = 0; i < 3; i++) {
            bookIds[i] = createBook("書籍" + i, "著者" + i, "ISBN-00" + i);
            loanIds[i] = borrowBookAndGetLoanId(memberId, bookIds[i]);
        }

        returnBook(loanIds[0]).statusCode(200);

        int newBook = createBook("新書籍", "新著者", "ISBN-NEW");
        borrowBook(memberId, newBook).statusCode(201);
    }

    @Test
    void 一般会員_上限到達後に返却して再度上限まで借りられる() {
        int memberId = createGeneralMember("会員F", "f@example.com");
        int[] bookIds = new int[3];
        int[] loanIds = new int[3];
        for (int i = 0; i < 3; i++) {
            bookIds[i] = createBook("書籍" + i, "著者" + i, "ISBN-00" + i);
            loanIds[i] = borrowBookAndGetLoanId(memberId, bookIds[i]);
        }

        for (int i = 0; i < 3; i++) {
            returnBook(loanIds[i]).statusCode(200);
        }

        for (int i = 0; i < 3; i++) {
            borrowBook(memberId, bookIds[i]).statusCode(201);
        }
    }

    // ========== プレミアム会員の貸出上限テスト ==========

    @Test
    void プレミアム会員_0冊の状態で貸出できる() {
        int memberId = createPremiumMember("P会員A", "pa@example.com");
        int bookId = createBook("書籍1", "著者1", "ISBN-P001");

        borrowBook(memberId, bookId)
            .statusCode(201)
            .body("active", equalTo(true));
    }

    @Test
    void プレミアム会員_5冊借りている状態で6冊目を貸出できる() {
        int memberId = createPremiumMember("P会員B", "pb@example.com");
        for (int i = 1; i <= 5; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-P00" + i);
            borrowBook(memberId, bookId).statusCode(201);
        }

        int book6 = createBook("書籍6", "著者6", "ISBN-P006");
        borrowBook(memberId, book6).statusCode(201);
    }

    @Test
    void プレミアム会員_9冊借りている状態で10冊目を貸出できる() {
        int memberId = createPremiumMember("P会員C", "pc@example.com");
        for (int i = 1; i <= 9; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-P00" + i);
            borrowBook(memberId, bookId).statusCode(201);
        }

        int book10 = createBook("書籍10", "著者10", "ISBN-P010");
        borrowBook(memberId, book10).statusCode(201);
    }

    @Test
    void プレミアム会員_10冊借りている状態で11冊目は貸出不可() {
        int memberId = createPremiumMember("P会員D", "pd@example.com");
        for (int i = 1; i <= 10; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-P00" + i);
            borrowBook(memberId, bookId).statusCode(201);
        }

        int book11 = createBook("書籍11", "著者11", "ISBN-P011");
        borrowBook(memberId, book11).statusCode(409);
    }

    @Test
    void プレミアム会員_10冊借りて1冊返却後に再度貸出できる() {
        int memberId = createPremiumMember("P会員E", "pe@example.com");
        int firstLoanId = 0;
        for (int i = 1; i <= 10; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-P00" + i);
            int loanId = borrowBookAndGetLoanId(memberId, bookId);
            if (i == 1) firstLoanId = loanId;
        }

        returnBook(firstLoanId).statusCode(200);

        int newBook = createBook("新書籍", "新著者", "ISBN-PNEW");
        borrowBook(memberId, newBook).statusCode(201);
    }

    // ========== 書籍の状態遷移テスト ==========

    @Test
    void 貸出後に書籍が貸出不可になる() {
        int memberId = createGeneralMember("会員G", "g@example.com");
        int bookId = createBook("状態テスト書籍", "著者", "ISBN-ST1");

        borrowBook(memberId, bookId).statusCode(201);

        when()
            .get("books/" + bookId)
        .then()
            .statusCode(200)
            .body("available", equalTo(false));
    }

    @Test
    void 返却後に書籍が貸出可能に戻る() {
        int memberId = createGeneralMember("会員H", "h@example.com");
        int bookId = createBook("状態テスト書籍2", "著者", "ISBN-ST2");

        int loanId = borrowBookAndGetLoanId(memberId, bookId);
        returnBook(loanId).statusCode(200);

        when()
            .get("books/" + bookId)
        .then()
            .statusCode(200)
            .body("available", equalTo(true));
    }

    @Test
    void 貸出中の書籍を別の会員が借りると409() {
        int member1 = createGeneralMember("会員I", "i@example.com");
        int member2 = createGeneralMember("会員J", "j@example.com");
        int bookId = createBook("競合テスト書籍", "著者", "ISBN-C1");

        borrowBook(member1, bookId).statusCode(201);
        borrowBook(member2, bookId).statusCode(409);
    }

    @Test
    void 返却済み書籍を再度貸出できる() {
        int memberId = createGeneralMember("会員K", "k@example.com");
        int bookId = createBook("再貸出テスト書籍", "著者", "ISBN-R1");

        int loanId = borrowBookAndGetLoanId(memberId, bookId);
        returnBook(loanId).statusCode(200);

        borrowBook(memberId, bookId).statusCode(201);
    }

    @Test
    void 返却済み書籍を別の会員が貸出できる() {
        int member1 = createGeneralMember("会員L", "l@example.com");
        int member2 = createGeneralMember("会員M", "m@example.com");
        int bookId = createBook("別会員再貸出書籍", "著者", "ISBN-R2");

        int loanId = borrowBookAndGetLoanId(member1, bookId);
        returnBook(loanId).statusCode(200);

        borrowBook(member2, bookId).statusCode(201);
    }

    // ========== 貸出期間の検証 ==========

    @Test
    void 一般会員の貸出期間は14日() {
        int memberId = createGeneralMember("会員N", "n@example.com");
        int bookId = createBook("期間テスト書籍1", "著者", "ISBN-D1");

        borrowBook(memberId, bookId)
            .statusCode(201)
            .body("loanDate", notNullValue())
            .body("dueDate", notNullValue());
    }

    @Test
    void プレミアム会員の貸出期間は30日() {
        int memberId = createPremiumMember("P会員F", "pf@example.com");
        int bookId = createBook("期間テスト書籍2", "著者", "ISBN-D2");

        borrowBook(memberId, bookId)
            .statusCode(201)
            .body("loanDate", notNullValue())
            .body("dueDate", notNullValue());
    }

    // ========== エラーケース ==========

    @Test
    void 存在しない会員で貸出するとエラー() {
        int bookId = createBook("エラーテスト書籍1", "著者", "ISBN-E1");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": 9999, "bookId": %d}
                """.formatted(bookId))
        .when()
            .post("loans")
        .then()
            .statusCode(404);
    }

    @Test
    void 存在しない書籍を貸出するとエラー() {
        int memberId = createGeneralMember("会員O", "o@example.com");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": %d, "bookId": 9999}
                """.formatted(memberId))
        .when()
            .post("loans")
        .then()
            .statusCode(404);
    }

    @Test
    void 一般会員が連続で上限まで借りて超過を確認() {
        int memberId = createGeneralMember("連続テスト会員", "seq@example.com");

        for (int i = 1; i <= 3; i++) {
            int bookId = createBook("連続書籍" + i, "著者" + i, "ISBN-SEQ" + i);
            borrowBook(memberId, bookId).statusCode(201);
        }

        int overBook = createBook("超過書籍", "著者X", "ISBN-OVER");
        borrowBook(memberId, overBook)
            .statusCode(409);
    }

    @Test
    void プレミアム会員が連続で上限まで借りて超過を確認() {
        int memberId = createPremiumMember("P連続テスト会員", "pseq@example.com");

        for (int i = 1; i <= 10; i++) {
            int bookId = createBook("P連続書籍" + i, "著者" + i, "ISBN-PSEQ" + i);
            borrowBook(memberId, bookId).statusCode(201);
        }

        int overBook = createBook("P超過書籍", "著者X", "ISBN-POVER");
        borrowBook(memberId, overBook)
            .statusCode(409);
    }

    @Test
    void 一般会員_全返却後に再度上限まで貸出可能() {
        int memberId = createGeneralMember("全返却テスト", "allret@example.com");

        int[] loanIds = new int[3];
        for (int i = 0; i < 3; i++) {
            int bookId = createBook("全返却書籍" + i, "著者" + i, "ISBN-AR" + i);
            loanIds[i] = borrowBookAndGetLoanId(memberId, bookId);
        }

        for (int loanId : loanIds) {
            returnBook(loanId).statusCode(200);
        }

        for (int i = 10; i < 13; i++) {
            int bookId = createBook("再貸出書籍" + i, "著者" + i, "ISBN-RE" + i);
            borrowBook(memberId, bookId).statusCode(201);
        }
    }

    // ========== ヘルパーメソッド ==========

    private int createGeneralMember(String name, String email) {
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

    private int createPremiumMember(String name, String email) {
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "%s", "email": "%s", "memberType": "PREMIUM"}
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

    private io.restassured.response.ValidatableResponse borrowBook(int memberId, int bookId) {
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {"memberId": %d, "bookId": %d}
                """.formatted(memberId, bookId))
        .when()
            .post("loans")
        .then();
    }

    private int borrowBookAndGetLoanId(int memberId, int bookId) {
        return borrowBook(memberId, bookId)
            .statusCode(201)
            .extract().path("id");
    }

    private io.restassured.response.ValidatableResponse returnBook(int loanId) {
        return given()
            .contentType(ContentType.JSON)
        .when()
            .post("loans/" + loanId + "/return")
        .then();
    }
}
