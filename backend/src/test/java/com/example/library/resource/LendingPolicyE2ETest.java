package com.example.library.resource;

import com.example.library.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LendingPolicyE2ETest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanAll();
    }

    @Test
    void 一般会員_0冊の状態で貸出できる() {
        String memberId = createGeneralMember("0000001", "会員A", "a@example.com");
        int bookId = createBook("書籍1", "著者1", "ISBN-001");

        var response = borrowBook(memberId, bookId);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(true, response.getBody().get("active"));
    }

    @Test
    void 一般会員_1冊借りている状態で2冊目を貸出できる() {
        String memberId = createGeneralMember("0000001", "会員B", "b@example.com");
        int book1 = createBook("書籍1", "著者1", "ISBN-001");
        int book2 = createBook("書籍2", "著者2", "ISBN-002");

        assertEquals(HttpStatus.CREATED, borrowBook(memberId, book1).getStatusCode());
        assertEquals(HttpStatus.CREATED, borrowBook(memberId, book2).getStatusCode());
    }

    @Test
    void 一般会員_2冊借りている状態で3冊目を貸出できる() {
        String memberId = createGeneralMember("0000001", "会員C", "c@example.com");
        int book1 = createBook("書籍1", "著者1", "ISBN-001");
        int book2 = createBook("書籍2", "著者2", "ISBN-002");
        int book3 = createBook("書籍3", "著者3", "ISBN-003");

        assertEquals(HttpStatus.CREATED, borrowBook(memberId, book1).getStatusCode());
        assertEquals(HttpStatus.CREATED, borrowBook(memberId, book2).getStatusCode());
        assertEquals(HttpStatus.CREATED, borrowBook(memberId, book3).getStatusCode());
    }

    @Test
    void 一般会員_3冊借りている状態で4冊目は貸出不可() {
        String memberId = createGeneralMember("0000001", "会員D", "d@example.com");
        for (int i = 1; i <= 3; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-00" + i);
            assertEquals(HttpStatus.CREATED, borrowBook(memberId, bookId).getStatusCode());
        }

        int book4 = createBook("書籍4", "著者4", "ISBN-004");
        assertEquals(HttpStatus.CONFLICT, borrowBook(memberId, book4).getStatusCode());
    }

    @Test
    void 一般会員_3冊借りて1冊返却後に再度貸出できる() {
        String memberId = createGeneralMember("0000001", "会員E", "e@example.com");
        int[] loanIds = new int[3];
        for (int i = 0; i < 3; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-00" + i);
            loanIds[i] = borrowBookAndGetLoanId(memberId, bookId);
        }

        assertEquals(HttpStatus.OK, returnBook(loanIds[0]).getStatusCode());

        int newBook = createBook("新書籍", "新著者", "ISBN-NEW");
        assertEquals(HttpStatus.CREATED, borrowBook(memberId, newBook).getStatusCode());
    }

    @Test
    void 一般会員_上限到達後に返却して再度上限まで借りられる() {
        String memberId = createGeneralMember("0000001", "会員F", "f@example.com");
        int[] bookIds = new int[3];
        int[] loanIds = new int[3];
        for (int i = 0; i < 3; i++) {
            bookIds[i] = createBook("書籍" + i, "著者" + i, "ISBN-00" + i);
            loanIds[i] = borrowBookAndGetLoanId(memberId, bookIds[i]);
        }

        for (int i = 0; i < 3; i++) {
            assertEquals(HttpStatus.OK, returnBook(loanIds[i]).getStatusCode());
        }

        for (int i = 0; i < 3; i++) {
            assertEquals(HttpStatus.CREATED, borrowBook(memberId, bookIds[i]).getStatusCode());
        }
    }

    @Test
    void プレミアム会員_0冊の状態で貸出できる() {
        String memberId = createPremiumMember("0000001", "P会員A", "pa@example.com");
        int bookId = createBook("書籍1", "著者1", "ISBN-P001");

        var response = borrowBook(memberId, bookId);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(true, response.getBody().get("active"));
    }

    @Test
    void プレミアム会員_5冊借りている状態で6冊目を貸出できる() {
        String memberId = createPremiumMember("0000001", "P会員B", "pb@example.com");
        for (int i = 1; i <= 5; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-P00" + i);
            assertEquals(HttpStatus.CREATED, borrowBook(memberId, bookId).getStatusCode());
        }

        int book6 = createBook("書籍6", "著者6", "ISBN-P006");
        assertEquals(HttpStatus.CREATED, borrowBook(memberId, book6).getStatusCode());
    }

    @Test
    void プレミアム会員_9冊借りている状態で10冊目を貸出できる() {
        String memberId = createPremiumMember("0000001", "P会員C", "pc@example.com");
        for (int i = 1; i <= 9; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-P00" + i);
            assertEquals(HttpStatus.CREATED, borrowBook(memberId, bookId).getStatusCode());
        }

        int book10 = createBook("書籍10", "著者10", "ISBN-P010");
        assertEquals(HttpStatus.CREATED, borrowBook(memberId, book10).getStatusCode());
    }

    @Test
    void プレミアム会員_10冊借りている状態で11冊目は貸出不可() {
        String memberId = createPremiumMember("0000001", "P会員D", "pd@example.com");
        for (int i = 1; i <= 10; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-P00" + i);
            assertEquals(HttpStatus.CREATED, borrowBook(memberId, bookId).getStatusCode());
        }

        int book11 = createBook("書籍11", "著者11", "ISBN-P011");
        assertEquals(HttpStatus.CONFLICT, borrowBook(memberId, book11).getStatusCode());
    }

    @Test
    void プレミアム会員_10冊借りて1冊返却後に再度貸出できる() {
        String memberId = createPremiumMember("0000001", "P会員E", "pe@example.com");
        int firstLoanId = 0;
        for (int i = 1; i <= 10; i++) {
            int bookId = createBook("書籍" + i, "著者" + i, "ISBN-P00" + i);
            int loanId = borrowBookAndGetLoanId(memberId, bookId);
            if (i == 1) firstLoanId = loanId;
        }

        assertEquals(HttpStatus.OK, returnBook(firstLoanId).getStatusCode());

        int newBook = createBook("新書籍", "新著者", "ISBN-PNEW");
        assertEquals(HttpStatus.CREATED, borrowBook(memberId, newBook).getStatusCode());
    }

    @Test
    void 貸出後に書籍が貸出不可になる() {
        String memberId = createGeneralMember("0000001", "会員G", "g@example.com");
        int bookId = createBook("状態テスト書籍", "著者", "ISBN-ST1");

        borrowBook(memberId, bookId);

        var response = restTemplate.getForEntity("/api/books/" + bookId, Map.class);
        assertEquals(false, response.getBody().get("available"));
    }

    @Test
    void 返却後に書籍が貸出可能に戻る() {
        String memberId = createGeneralMember("0000001", "会員H", "h@example.com");
        int bookId = createBook("状態テスト書籍2", "著者", "ISBN-ST2");

        int loanId = borrowBookAndGetLoanId(memberId, bookId);
        returnBook(loanId);

        var response = restTemplate.getForEntity("/api/books/" + bookId, Map.class);
        assertEquals(true, response.getBody().get("available"));
    }

    @Test
    void 貸出中の書籍を別の会員が借りると409() {
        String member1 = createGeneralMember("0000001", "会員I", "i@example.com");
        String member2 = createGeneralMember("0000002", "会員J", "j@example.com");
        int bookId = createBook("競合テスト書籍", "著者", "ISBN-C1");

        assertEquals(HttpStatus.CREATED, borrowBook(member1, bookId).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, borrowBook(member2, bookId).getStatusCode());
    }

    @Test
    void 返却済み書籍を再度貸出できる() {
        String memberId = createGeneralMember("0000001", "会員K", "k@example.com");
        int bookId = createBook("再貸出テスト書籍", "著者", "ISBN-R1");

        int loanId = borrowBookAndGetLoanId(memberId, bookId);
        returnBook(loanId);

        assertEquals(HttpStatus.CREATED, borrowBook(memberId, bookId).getStatusCode());
    }

    @Test
    void 返却済み書籍を別の会員が貸出できる() {
        String member1 = createGeneralMember("0000001", "会員L", "l@example.com");
        String member2 = createGeneralMember("0000002", "会員M", "m@example.com");
        int bookId = createBook("別会員再貸出書籍", "著者", "ISBN-R2");

        int loanId = borrowBookAndGetLoanId(member1, bookId);
        returnBook(loanId);

        assertEquals(HttpStatus.CREATED, borrowBook(member2, bookId).getStatusCode());
    }

    @Test
    void 一般会員の貸出期間は14日() {
        String memberId = createGeneralMember("0000001", "会員N", "n@example.com");
        int bookId = createBook("期間テスト書籍1", "著者", "ISBN-D1");

        var response = borrowBook(memberId, bookId);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().get("loanDate"));
        assertNotNull(response.getBody().get("dueDate"));
    }

    @Test
    void プレミアム会員の貸出期間は30日() {
        String memberId = createPremiumMember("0000001", "P会員F", "pf@example.com");
        int bookId = createBook("期間テスト書籍2", "著者", "ISBN-D2");

        var response = borrowBook(memberId, bookId);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().get("loanDate"));
        assertNotNull(response.getBody().get("dueDate"));
    }

    @Test
    void 存在しない会員で貸出するとエラー() {
        int bookId = createBook("エラーテスト書籍1", "著者", "ISBN-E1");

        var response = restTemplate.postForEntity("/api/loans",
                Map.of("memberId", "9999999", "bookId", bookId), Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void 存在しない書籍を貸出するとエラー() {
        String memberId = createGeneralMember("0000001", "会員O", "o@example.com");

        var response = restTemplate.postForEntity("/api/loans",
                Map.of("memberId", memberId, "bookId", 9999), Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void 一般会員が連続で上限まで借りて超過を確認() {
        String memberId = createGeneralMember("0000001", "連続テスト会員", "seq@example.com");

        for (int i = 1; i <= 3; i++) {
            int bookId = createBook("連続書籍" + i, "著者" + i, "ISBN-SEQ" + i);
            assertEquals(HttpStatus.CREATED, borrowBook(memberId, bookId).getStatusCode());
        }

        int overBook = createBook("超過書籍", "著者X", "ISBN-OVER");
        assertEquals(HttpStatus.CONFLICT, borrowBook(memberId, overBook).getStatusCode());
    }

    @Test
    void プレミアム会員が連続で上限まで借りて超過を確認() {
        String memberId = createPremiumMember("0000001", "P連続テスト会員", "pseq@example.com");

        for (int i = 1; i <= 10; i++) {
            int bookId = createBook("P連続書籍" + i, "著者" + i, "ISBN-PSEQ" + i);
            assertEquals(HttpStatus.CREATED, borrowBook(memberId, bookId).getStatusCode());
        }

        int overBook = createBook("P超過書籍", "著者X", "ISBN-POVER");
        assertEquals(HttpStatus.CONFLICT, borrowBook(memberId, overBook).getStatusCode());
    }

    @Test
    void 一般会員_全返却後に再度上限まで貸出可能() {
        String memberId = createGeneralMember("0000001", "全返却テスト", "allret@example.com");

        int[] loanIds = new int[3];
        for (int i = 0; i < 3; i++) {
            int bookId = createBook("全返却書籍" + i, "著者" + i, "ISBN-AR" + i);
            loanIds[i] = borrowBookAndGetLoanId(memberId, bookId);
        }

        for (int loanId : loanIds) {
            assertEquals(HttpStatus.OK, returnBook(loanId).getStatusCode());
        }

        for (int i = 10; i < 13; i++) {
            int bookId = createBook("再貸出書籍" + i, "著者" + i, "ISBN-RE" + i);
            assertEquals(HttpStatus.CREATED, borrowBook(memberId, bookId).getStatusCode());
        }
    }

    // ========== ヘルパーメソッド ==========

    private String createGeneralMember(String id, String name, String email) {
        var response = restTemplate.postForEntity("/api/members",
                Map.of("id", id, "name", name, "email", email, "memberType", "GENERAL"), Map.class);
        return (String) response.getBody().get("id");
    }

    private String createPremiumMember(String id, String name, String email) {
        var response = restTemplate.postForEntity("/api/members",
                Map.of("id", id, "name", name, "email", email, "memberType", "PREMIUM"), Map.class);
        return (String) response.getBody().get("id");
    }

    private int createBook(String title, String author, String isbn) {
        var response = restTemplate.postForEntity("/api/books",
                Map.of("title", title, "author", author, "isbn", isbn), Map.class);
        return (int) response.getBody().get("id");
    }

    private ResponseEntity<Map> borrowBook(String memberId, int bookId) {
        return restTemplate.postForEntity("/api/loans",
                Map.of("memberId", memberId, "bookId", bookId), Map.class);
    }

    private int borrowBookAndGetLoanId(String memberId, int bookId) {
        var response = borrowBook(memberId, bookId);
        return (int) response.getBody().get("id");
    }

    private ResponseEntity<Map> returnBook(int loanId) {
        return restTemplate.postForEntity("/api/loans/" + loanId + "/return", null, Map.class);
    }
}
