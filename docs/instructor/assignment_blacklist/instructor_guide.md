# インストラクターガイド: 会員ブラックリスト照会機能

本ドキュメントは、課題の各ステップで期待されるコードと指導上のポイントをまとめたものです。

---

## ファイル一覧

### 新規作成

| ファイル | パッケージ / パス | ステップ |
|---------|-----------------|---------|
| `BlacklistStatus.java` | `domain/model/` | 2 |
| `BlacklistCheckPort.java` | `domain/` | 2 |
| `FakeBlacklistCheckPort.java` | `src/test` ─ `domain/` | 2 |
| `BlacklistCheckPortTest.java` | `src/test` ─ `domain/` | 2 |
| `ExternalBlacklistAdapter.java` | `infrastructure/` | 4 |
| `.gitea/workflows/ci.yaml` | プロジェクトルート | 6 |

### 変更

| ファイル | 変更内容 | ステップ |
|---------|---------|---------|
| `LoanService.java` | `BlacklistCheckPort` の注入とチェック追加 | 3 |
| `LoanServiceTest.java` | `@TestConfiguration` 追加、テストケース3件追加 | 3 |
| `LoanControllerTest.java` | `@TestConfiguration` 追加、テストケース1件追加 | 4 |
| `application.properties` | `blacklist.service.url` 追加 | 4 |

---

## ステップ2: ドメイン層のTDD

### 期待されるコード

#### `BlacklistStatus.java`

```java
package com.example.library.domain.model;

public record BlacklistStatus(String email, boolean blacklisted, String reason) {
}
```

#### `BlacklistCheckPort.java`

```java
package com.example.library.domain;

import com.example.library.domain.model.BlacklistStatus;

public interface BlacklistCheckPort {
    BlacklistStatus check(String email);
}
```

#### `FakeBlacklistCheckPort.java`（src/test/java に配置）

```java
package com.example.library.domain;

import com.example.library.domain.model.BlacklistStatus;

public class FakeBlacklistCheckPort implements BlacklistCheckPort {

    private boolean blacklisted = false;
    private String reason = null;
    private RuntimeException exceptionToThrow = null;

    @Override
    public BlacklistStatus check(String email) {
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }
        return new BlacklistStatus(email, blacklisted, reason);
    }

    public void setBlacklisted(boolean blacklisted, String reason) {
        this.blacklisted = blacklisted;
        this.reason = reason;
        this.exceptionToThrow = null;
    }

    public void setExceptionToThrow(RuntimeException exception) {
        this.exceptionToThrow = exception;
    }

    public void reset() {
        this.blacklisted = false;
        this.reason = null;
        this.exceptionToThrow = null;
    }
}
```

#### `BlacklistCheckPortTest.java`（src/test/java に配置）

```java
package com.example.library.domain;

import com.example.library.domain.model.BlacklistStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlacklistCheckPortTest {

    @Test
    void ブラックリストに該当する会員を照会すると該当ありが返る() {
        FakeBlacklistCheckPort fake = new FakeBlacklistCheckPort();
        fake.setBlacklisted(true, "延滞金未納");

        BlacklistStatus status = fake.check("blocked@example.com");

        assertTrue(status.blacklisted());
        assertEquals("blocked@example.com", status.email());
        assertEquals("延滞金未納", status.reason());
    }

    @Test
    void ブラックリストに該当しない会員を照会すると該当なしが返る() {
        FakeBlacklistCheckPort fake = new FakeBlacklistCheckPort();

        BlacklistStatus status = fake.check("good@example.com");

        assertFalse(status.blacklisted());
        assertEquals("good@example.com", status.email());
        assertNull(status.reason());
    }
}
```

### テスト実行結果

```
mvn test -pl backend -Dtest="*BlacklistCheckPortTest"
```

```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 指導ポイント

- **Red フェーズでコンパイルエラーが出て戸惑う参加者がいる場合:**
  「TDDではテストが先なので、まだ存在しないクラスを参照してコンパイルエラーになるのは正常です。次の Green フェーズでプロダクションコードを作ってエラーを解消します」と説明する。

- **`record` を知らない参加者がいる場合:**
  `record` は Java 16 から導入されたデータ保持用のクラス宣言。コンストラクタ、getter、`equals`、`hashCode`、`toString` が自動生成される。通常の `class` で書いても問題ない。

- **フェイクの設計について質問があった場合:**
  フェイクの設計に正解は1つではない。コンストラクタで振る舞いを渡す方式、setter で後から設定する方式、サブクラスで振る舞いを変える方式など複数ある。ここでは setter 方式を採用しているが、他の方式でも構わない。

---

## ステップ3: サービス層のTDD

### 期待されるコード

#### `LoanService.java`（変更後の全体）

```java
package com.example.library.application;

import com.example.library.domain.BlacklistCheckPort;
import com.example.library.domain.LendingPolicy;
import com.example.library.domain.model.BlacklistStatus;
import com.example.library.domain.model.Book;
import com.example.library.domain.model.Loan;
import com.example.library.domain.model.Member;
import com.example.library.domain.repository.BookRepository;
import com.example.library.domain.repository.LoanRepository;
import com.example.library.domain.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LoanService {

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final BlacklistCheckPort blacklistCheckPort;      // ← 追加

    public LoanService(BookRepository bookRepository,
                       MemberRepository memberRepository,
                       LoanRepository loanRepository,
                       BlacklistCheckPort blacklistCheckPort) {  // ← 追加
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
        this.blacklistCheckPort = blacklistCheckPort;           // ← 追加
    }

    public Loan borrowBook(Long memberId, Long bookId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません: ID=" + memberId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: ID=" + bookId));

        if (!book.isAvailable()) {
            throw new IllegalStateException("この書籍は現在貸出中です");
        }

        checkBlacklist(member);                                 // ← 追加

        List<Loan> activeLoans = loanRepository.findByMemberIdAndReturnDateIsNull(memberId);
        if (!LendingPolicy.canBorrow(member.getMemberType(), activeLoans.size())) {
            throw new IllegalStateException("貸出上限に達しています（上限: "
                    + LendingPolicy.getMaxLoans(member.getMemberType()) + "冊）");
        }

        LocalDate loanDate = LocalDate.now();
        LocalDate dueDate = LendingPolicy.calculateDueDate(member.getMemberType(), loanDate);

        book.checkout();
        bookRepository.save(book);

        Loan loan = new Loan(bookId, memberId, loanDate, dueDate);
        return loanRepository.save(loan);
    }

    // ── ここから追加 ──────────────────────────────────────

    private void checkBlacklist(Member member) {
        BlacklistStatus status = null;
        try {
            status = blacklistCheckPort.check(member.getEmail());
        } catch (RuntimeException e) {
            // 外部サービスの障害時は貸出を許可する（フェイルオープン）
        }
        if (status != null && status.blacklisted()) {
            throw new IllegalStateException(
                    "ブラックリストに登録されている会員です: " + member.getEmail());
        }
    }

    // ── ここまで追加 ──────────────────────────────────────

    public Loan returnBook(Long loanId) {
        return returnBook(loanId, false);
    }

    public Loan returnBook(Long loanId, boolean isBookPostReturn) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("貸出記録が見つかりません: ID=" + loanId));

        Book book = bookRepository.findById(loan.getBookId())
                .orElseThrow(() -> new IllegalStateException("書籍が見つかりません: ID=" + loan.getBookId()));

        Member member = memberRepository.findById(loan.getMemberId())
                .orElseThrow(() -> new IllegalStateException("会員が見つかりません: ID=" + loan.getMemberId()));

        LocalDate returnDate = LocalDate.now();
        if (isBookPostReturn) {
            returnDate = returnDate.minusDays(1);
        }

        loan.returnBook(returnDate);
        book.returnBook();

        int overdueFee = LendingPolicy.calculateOverdueFee(member.getMemberType(), loan.getDueDate(), returnDate);
        loan.setOverdueFee(overdueFee);

        bookRepository.save(book);
        return loanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public List<Loan> findAll() {
        return loanRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Loan findById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("貸出記録が見つかりません: ID=" + id));
    }
}
```

#### `LoanServiceTest.java`（変更後の全体）

```java
package com.example.library.application;

import com.example.library.TestDatabaseCleaner;
import com.example.library.domain.BlacklistCheckPort;
import com.example.library.domain.FakeBlacklistCheckPort;
import com.example.library.domain.model.Book;
import com.example.library.domain.model.Loan;
import com.example.library.domain.model.Member;
import com.example.library.domain.model.MemberType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoanServiceTest {

    // ── ここから追加 ──────────────────────────────────────

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public FakeBlacklistCheckPort blacklistCheckPort() {
            return new FakeBlacklistCheckPort();
        }
    }

    // ── ここまで追加 ──────────────────────────────────────

    @Autowired
    LoanService loanService;

    @Autowired
    BookService bookService;

    @Autowired
    MemberService memberService;

    @Autowired
    TestDatabaseCleaner dbCleaner;

    @Autowired                                                  // ← 追加
    FakeBlacklistCheckPort fakeBlacklistCheckPort;              // ← 追加

    private Member generalMember;
    private Book book;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();
        fakeBlacklistCheckPort.reset();                         // ← 追加
        generalMember = memberService.create("田中太郎", "tanaka@example.com", MemberType.GENERAL);
        book = bookService.create("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");
    }

    @Test
    void 書籍を貸出できる() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());

        assertNotNull(loan.getId());
        assertEquals(book.getId(), loan.getBookId());
        assertEquals(generalMember.getId(), loan.getMemberId());
        assertNotNull(loan.getLoanDate());
        assertNotNull(loan.getDueDate());
        assertNull(loan.getReturnDate());
        assertTrue(loan.isActive());
    }

    @Test
    void 貸出すると書籍が貸出不可になる() {
        loanService.borrowBook(generalMember.getId(), book.getId());

        Book found = bookService.findById(book.getId());
        assertFalse(found.isAvailable());
    }

    @Test
    void 貸出中の書籍は借りられない() {
        loanService.borrowBook(generalMember.getId(), book.getId());

        Member anotherMember = memberService.create("鈴木", "suzuki@example.com", MemberType.GENERAL);
        assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(anotherMember.getId(), book.getId()));
    }

    @Test
    void 一般会員は3冊まで借りられる() {
        Book book2 = bookService.create("書籍2", "著者2", "ISBN-2");
        Book book3 = bookService.create("書籍3", "著者3", "ISBN-3");

        loanService.borrowBook(generalMember.getId(), book.getId());
        loanService.borrowBook(generalMember.getId(), book2.getId());
        loanService.borrowBook(generalMember.getId(), book3.getId());

        assertEquals(3, loanService.findAll().stream()
                .filter(Loan::isActive)
                .filter(l -> l.getMemberId().equals(generalMember.getId()))
                .count());
    }

    @Test
    void 一般会員は4冊目を借りると例外() {
        Book book2 = bookService.create("書籍2", "著者2", "ISBN-2");
        Book book3 = bookService.create("書籍3", "著者3", "ISBN-3");
        Book book4 = bookService.create("書籍4", "著者4", "ISBN-4");

        loanService.borrowBook(generalMember.getId(), book.getId());
        loanService.borrowBook(generalMember.getId(), book2.getId());
        loanService.borrowBook(generalMember.getId(), book3.getId());

        assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(generalMember.getId(), book4.getId()));
    }

    @Test
    void プレミアム会員は10冊まで借りられる() {
        Member premiumMember = memberService.create("高橋", "takahashi@example.com", MemberType.PREMIUM);

        for (int i = 0; i < 10; i++) {
            Book b = bookService.create("書籍" + i, "著者" + i, "ISBN-" + i);
            loanService.borrowBook(premiumMember.getId(), b.getId());
        }

        assertEquals(10, loanService.findAll().stream()
                .filter(Loan::isActive)
                .filter(l -> l.getMemberId().equals(premiumMember.getId()))
                .count());
    }

    @Test
    void 書籍を返却できる() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());

        Loan returned = loanService.returnBook(loan.getId());

        assertNotNull(returned.getReturnDate());
        assertFalse(returned.isActive());
    }

    @Test
    void 返却すると書籍が貸出可能に戻る() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());

        loanService.returnBook(loan.getId());

        Book found = bookService.findById(book.getId());
        assertTrue(found.isAvailable());
    }

    @Test
    void 返却後は再度借りられる() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());
        loanService.returnBook(loan.getId());

        Loan newLoan = loanService.borrowBook(generalMember.getId(), book.getId());

        assertNotNull(newLoan.getId());
        assertTrue(newLoan.isActive());
    }

    @Test
    void 存在しない会員で貸出すると例外() {
        assertThrows(IllegalArgumentException.class,
                () -> loanService.borrowBook(9999L, book.getId()));
    }

    @Test
    void 存在しない書籍を貸出すると例外() {
        assertThrows(IllegalArgumentException.class,
                () -> loanService.borrowBook(generalMember.getId(), 9999L));
    }

    // ── ここから追加 ──────────────────────────────────────

    @Test
    void ブラックリスト非該当の会員は貸出できる() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());

        assertNotNull(loan.getId());
        assertTrue(loan.isActive());
    }

    @Test
    void ブラックリスト該当の会員は貸出できない() {
        fakeBlacklistCheckPort.setBlacklisted(true, "延滞金未納");

        assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(generalMember.getId(), book.getId()));
    }

    @Test
    void 外部サービス障害時はフェイルオープンで貸出できる() {
        fakeBlacklistCheckPort.setExceptionToThrow(new RuntimeException("Service Unavailable"));

        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());

        assertNotNull(loan.getId());
        assertTrue(loan.isActive());
    }

    // ── ここまで追加 ──────────────────────────────────────
}
```

### テスト実行結果

```
mvn test -pl backend -Dtest="*LoanServiceTest"
```

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 指導ポイント

- **`LoanService` のコンストラクタに `BlacklistCheckPort` を追加すると既存テストが壊れる:**
  これは想定通り。Spring が `BlacklistCheckPort` のBeanを見つけられずにエラーになる。`@TestConfiguration` で `FakeBlacklistCheckPort` をBeanとして登録することで解決する。参加者がここで詰まった場合は、「Spring は `LoanService` を作るとき、コンストラクタの引数をすべてBeanとして見つける必要がある」と説明する。

- **フェイルオープンの実装でよくある間違い:**
  課題で意図的にヒントとして提示している「問題のあるコード」は以下:

  ```java
  try {
      BlacklistStatus status = blacklistCheckPort.check(member.getEmail());
      if (status.blacklisted()) {
          throw new IllegalStateException("...");  // ← これも RuntimeException
      }
  } catch (RuntimeException e) {
      // ここで IllegalStateException も握りつぶされる！
  }
  ```

  `IllegalStateException` は `RuntimeException` のサブクラスなので、ブラックリスト該当時にスローした例外も `catch` ブロックで握りつぶされる。テストケース2（ブラックリスト該当で例外が出るべきテスト）が失敗して、この問題に気づけるようになっている。

  **正しい実装（2パターン）:**

  パターンA ─ try の外で判定する（推奨）:
  ```java
  BlacklistStatus status = null;
  try {
      status = blacklistCheckPort.check(member.getEmail());
  } catch (RuntimeException e) {
      // フェイルオープン
  }
  if (status != null && status.blacklisted()) {
      throw new IllegalStateException("...");
  }
  ```

  パターンB ─ catch で再スローする:
  ```java
  try {
      BlacklistStatus status = blacklistCheckPort.check(member.getEmail());
      if (status.blacklisted()) {
          throw new IllegalStateException("...");
      }
  } catch (IllegalStateException e) {
      throw e;
  } catch (RuntimeException e) {
      // フェイルオープン
  }
  ```

  パターンAの方がシンプルで、例外の階層を気にする必要がないため推奨。

- **`@Primary` の役割について質問があった場合:**
  ステップ4で `ExternalBlacklistAdapter`（`@Component`）を追加すると、`BlacklistCheckPort` のBeanが2つ存在することになる。`@Primary` を付けると、テスト時にはフェイクが優先される。ステップ3時点では `ExternalBlacklistAdapter` がまだ存在しないため `@Primary` は不要だが、先に付けておくことでステップ4でのトラブルを防げる。

---

## ステップ4: E2Eテスト

### 期待されるコード

#### `ExternalBlacklistAdapter.java`（新規）

```java
package com.example.library.infrastructure;

import com.example.library.domain.BlacklistCheckPort;
import com.example.library.domain.model.BlacklistStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExternalBlacklistAdapter implements BlacklistCheckPort {

    private final RestClient restClient;

    public ExternalBlacklistAdapter(@Value("${blacklist.service.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public BlacklistStatus check(String email) {
        return restClient.get()
                .uri("/api/blacklist/check?email={email}", email)
                .retrieve()
                .body(BlacklistStatus.class);
    }
}
```

#### `application.properties`（追記）

```properties
# 末尾に追加
blacklist.service.url=${BLACKLIST_SERVICE_URL:http://localhost:8081}
```

#### `LoanControllerTest.java`（変更後の全体）

```java
package com.example.library.resource;

import com.example.library.TestDatabaseCleaner;
import com.example.library.domain.FakeBlacklistCheckPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoanControllerTest {

    // ── ここから追加 ──────────────────────────────────────

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public FakeBlacklistCheckPort blacklistCheckPort() {
            return new FakeBlacklistCheckPort();
        }
    }

    // ── ここまで追加 ──────────────────────────────────────

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDatabaseCleaner cleaner;

    @Autowired                                                  // ← 追加
    FakeBlacklistCheckPort fakeBlacklistCheckPort;              // ← 追加

    @BeforeEach
    void setUp() {
        cleaner.cleanAll();
        fakeBlacklistCheckPort.reset();                         // ← 追加
    }

    @Test
    void 書籍を貸出できる() {
        int memberId = createMember("田中太郎", "tanaka@example.com");
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
        int memberId = createMember("田中太郎", "tanaka@example.com");
        int bookId = createBook("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        restTemplate.postForEntity("/api/loans",
                Map.of("memberId", memberId, "bookId", bookId), Map.class);

        var response = restTemplate.getForEntity("/api/loans", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().size() >= 1);
    }

    @Test
    void 書籍を返却できる() {
        int memberId = createMember("田中太郎", "tanaka@example.com");
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
        int memberId = createMember("田中太郎", "tanaka@example.com");
        int bookId = createBook("書籍X", "著者X", "ISBN-X");

        restTemplate.postForEntity("/api/loans",
                Map.of("memberId", memberId, "bookId", bookId), Map.class);

        int anotherMemberId = createMember("鈴木", "suzuki@example.com");
        var response = restTemplate.postForEntity("/api/loans",
                Map.of("memberId", anotherMemberId, "bookId", bookId), Map.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    // ── ここから追加 ──────────────────────────────────────

    @Test
    void ブラックリスト該当会員が貸出すると409が返る() {
        int memberId = createMember("田中太郎", "tanaka@example.com");
        int bookId = createBook("テスト駆動開発", "Kent Beck", "978-4-274-21788-0");

        fakeBlacklistCheckPort.setBlacklisted(true, "延滞金未納");

        var response = restTemplate.postForEntity("/api/loans",
                Map.of("memberId", memberId, "bookId", bookId), Map.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    // ── ここまで追加 ──────────────────────────────────────

    private int createMember(String name, String email) {
        var response = restTemplate.postForEntity("/api/members",
                Map.of("name", name, "email", email, "memberType", "GENERAL"), Map.class);
        return (int) response.getBody().get("id");
    }

    private int createBook(String title, String author, String isbn) {
        var response = restTemplate.postForEntity("/api/books",
                Map.of("title", title, "author", author, "isbn", isbn), Map.class);
        return (int) response.getBody().get("id");
    }
}
```

### テスト実行結果

```
# ユニットテスト + 統合テスト
mvn test -pl backend
```

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```
# E2Eテストを含む全テスト
mvn test -pl backend -Dsurefire.excludedGroups=""
```

```
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

（テスト件数は既存テストの数により変動）

### 指導ポイント

- **`@TestConfiguration` と `@Primary` の組み合わせ:**
  `ExternalBlacklistAdapter`（`@Component`）と `FakeBlacklistCheckPort`（`@TestConfiguration @Bean`）の2つが `BlacklistCheckPort` を実装している。`@Primary` によりテスト時はフェイクが優先される。

- **E2Eテストでのフェイクの状態管理:**
  `FakeBlacklistCheckPort` はSpringコンテキスト内で共有されるBeanのため、テストメソッド間で状態が残る可能性がある。`@BeforeEach` で `reset()` を呼ぶことで、各テストが独立して動くようにしている。

- **`RestClient` を知らない参加者がいる場合:**
  Spring Boot 3.2 以降で推奨される HTTP クライアント。`RestTemplate` の後継にあたる。`RestTemplate` で実装しても問題ない。

- **E2Eテストで既存テストが壊れた場合:**
  `@TestConfiguration` を追加すると、Springコンテキストのキャッシュキーが変わり、別のコンテキストが生成される。他のE2Eテストクラス（`LendingPolicyE2ETest` 等）にも同様の `@TestConfiguration` が必要になる場合がある。壊れた場合はそのテストクラスにも `@TestConfiguration` を追加するか、共通のテスト用設定クラスを `src/test/java` に切り出すことを検討する。

---

## ステップ5: SonarQube静的解析

### 想定される主な指摘事項

| 種別 | 指摘内容 | 該当箇所 | 対応例 |
|------|---------|---------|--------|
| Code Smell | 空の `catch` ブロック | `LoanService.checkBlacklist()` | ログ出力を追加する（`logger.warn("...", e)`） |
| Code Smell | ロガーが未定義 | `LoanService` | `private static final Logger` を追加 |
| Coverage | `ExternalBlacklistAdapter` のカバレッジ不足 | `infrastructure/` | E2Eテストではフェイクに差し替えているため、本物の HTTP 通信コードはカバーされない。これは意図的 |

### 指導ポイント

- **空の catch ブロックの指摘:**
  SonarQube は空の `catch` ブロックを Code Smell として報告する。フェイルオープンのため意図的に例外を無視しているが、ログ出力は運用上重要。以下のようにリファクタリングするのが望ましい:

  ```java
  private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

  private void checkBlacklist(Member member) {
      BlacklistStatus status = null;
      try {
          status = blacklistCheckPort.check(member.getEmail());
      } catch (RuntimeException e) {
          logger.warn("ブラックリストサービスへの照会に失敗しました: {}", e.getMessage());
      }
      if (status != null && status.blacklisted()) {
          throw new IllegalStateException(
                  "ブラックリストに登録されている会員です: " + member.getEmail());
      }
  }
  ```

- **カバレッジの解釈:**
  `ExternalBlacklistAdapter` のカバレッジが低いのは、テストでフェイクに差し替えているため。これは正しいテスト設計の結果であり、カバレッジのために外部サービスを呼ぶテストを書くべきではない。SonarQube の数値を盲信せず、テスト設計の意図を理解することが重要。

---

## ステップ6: CIパイプライン

### 期待されるワークフロー

#### `.gitea/workflows/ci.yaml`

```yaml
name: CI

on:
  push:
    branches: ['*']

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: library
          POSTGRES_USER: library
          POSTGRES_PASSWORD: library
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Build
        run: mvn compile -pl backend

      - name: Unit Tests
        run: mvn test -pl backend
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/library
          DATABASE_USER: library
          DATABASE_PASSWORD: library

      - name: E2E Tests
        run: mvn test -pl backend -Dsurefire.excludedGroups=""
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/library
          DATABASE_USER: library
          DATABASE_PASSWORD: library

      - name: SonarQube Analysis
        run: |
          mvn sonar:sonar -pl backend \
            -Dsonar.projectKey=library \
            -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
            -Dsonar.token=${{ secrets.SONAR_TOKEN }}
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/library
          DATABASE_USER: library
          DATABASE_PASSWORD: library
```

### 指導ポイント

- **Gitea Actions と GitHub Actions の違い:**
  書式はほぼ同じだが、利用可能な Action（`actions/checkout` 等）は Gitea のインスタンス設定に依存する。環境に合わせて `uses:` のURLを調整する必要がある場合がある。

- **`services` セクション:**
  PostgreSQL コンテナをサービスとして起動する。テストはこのコンテナに接続する。`health-cmd` を設定しておくことで、PostgreSQL が起動完了するまで次のステップを待機できる。

- **SonarQube のシークレット設定:**
  Gitea のリポジトリ設定 → Secrets から `SONAR_HOST_URL` と `SONAR_TOKEN` を登録する必要がある。講師側で事前に設定しておくか、手順を案内する。

- **よくあるCI失敗パターン:**
  - PostgreSQL の接続エラー → `DATABASE_URL` 等の環境変数が正しいか確認
  - `actions/setup-java` が見つからない → Gitea のミラー設定を確認
  - SonarQube の接続エラー → シークレットの設定ミス

---

## トラブルシューティング

### よくある質問と対応

| 症状 | 原因 | 対応 |
|------|------|------|
| `LoanService` のコンストラクタでエラー | `BlacklistCheckPort` のBeanが見つからない | `@TestConfiguration` でフェイクをBean登録する |
| ブラックリスト該当テストが通ってしまう | `catch (RuntimeException e)` で `IllegalStateException` も握りつぶされている | `try` の外で `blacklisted()` を判定する |
| E2Eテストで既存テストが壊れる | `ExternalBlacklistAdapter` がBean登録されたが外部サービスに接続できない | `@TestConfiguration` + `@Primary` でフェイクを優先させる |
| `BlacklistStatus` のデシリアライズエラー | JSON のフィールド名と record のコンポーネント名が不一致 | 外部サービスのレスポンス JSON と `record` のフィールド名を一致させる |
| `FakeBlacklistCheckPort` を `@Autowired` できない | `@Bean` の戻り値型が `BlacklistCheckPort` になっている | 戻り値型を `FakeBlacklistCheckPort` に変更する |
