# インストラクターガイド: 貸出延長機能

本ドキュメントは、課題の各ステップで期待されるコードと指導上のポイントをまとめたものです。

---

## ファイル一覧

### 新規作成

| ファイル | パッケージ / パス | ステップ |
|---------|-----------------|---------|
| `LoanExtensionPort.java` | `domain/` | 2 |
| `FakeLoanExtensionPort.java` | `src/test` ─ `domain/` | 2 |
| `LoanExtensionPortTest.java` | `src/test` ─ `domain/` | 2 |
| `DatabaseLoanExtensionAdapter.java` | `infrastructure/` | 4 |

### 変更

| ファイル | 変更内容 | ステップ |
|---------|---------|---------|
| `LoanService.java` | `LoanExtensionPort` の注入と `extendLoan()` の実装 | 3 |
| `LoanServiceTest.java` | `@TestConfiguration` 追加、テストケース2件追加 | 3 |

---

## ステップ2: ドメイン層のTDD

### 期待されるコード

#### `LoanExtensionPort.java`

```java
package com.example.library.domain;

import java.time.LocalDate;

public interface LoanExtensionPort {
    void saveExtension(Long loanId, LocalDate newDueDate);
}
```

#### `FakeLoanExtensionPort.java`（src/test/java に配置）

```java
package com.example.library.domain;

import java.time.LocalDate;

public class FakeLoanExtensionPort implements LoanExtensionPort {

    private boolean called = false;
    private Long lastLoanId;
    private LocalDate lastNewDueDate;

    @Override
    public void saveExtension(Long loanId, LocalDate newDueDate) {
        this.called = true;
        this.lastLoanId = loanId;
        this.lastNewDueDate = newDueDate;
    }

    public boolean wasCalled() {
        return called;
    }

    public Long getLastLoanId() {
        return lastLoanId;
    }

    public LocalDate getLastNewDueDate() {
        return lastNewDueDate;
    }

    public void reset() {
        this.called = false;
        this.lastLoanId = null;
        this.lastNewDueDate = null;
    }
}
```

#### `LoanExtensionPortTest.java`（src/test/java に配置）

```java
package com.example.library.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LoanExtensionPortTest {

    @Test
    void 延長データの保存が正しい引数で呼び出される() {
        FakeLoanExtensionPort fake = new FakeLoanExtensionPort();

        fake.saveExtension(1L, LocalDate.of(2025, 2, 8));

        assertTrue(fake.wasCalled());
        assertEquals(1L, fake.getLastLoanId());
        assertEquals(LocalDate.of(2025, 2, 8), fake.getLastNewDueDate());
    }
}
```

### テスト実行結果

```
cd backend
mvn test -Dtest="*LoanExtensionPortTest"
```

```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 指導ポイント

- **Red フェーズでコンパイルエラーが出て戸惑う参加者がいる場合:**
  「TDDではテストが先なので、まだ存在しないクラスを参照してコンパイルエラーになるのは正常です。次の Green フェーズでプロダクションコードを作ってエラーを解消します」と説明する。

- **フェイクの設計について質問があった場合:**
  フェイクの設計に正解は1つではない。ここでは「呼び出しを記録する」方式を採用している。ポートが `void` メソッドのため、戻り値ではなく「正しい引数で呼ばれたか」を検証する。

- **テストケースが1件しかないことについて質問があった場合:**
  ドメイン層のテストでは、フェイクが正しく呼び出しを記録することだけを確認すれば十分。エラーケース（返却済み・延長済み）のテストはステップ3のサービス層で行う。

- **ポートが `void` であることについて質問があった場合:**
  ポートの役割は「延長データをDBに保存する」こと。サービス側がエンティティの状態変更（`dueDate` + 7日、`extended` = true）と保存を行い、ポートは追加のDB操作（延長の記録）を担当する。`void` にすることで、サービスがエンティティの状態を完全に管理でき、フェイクとの組み合わせでもテストが正しく動作する。

---

## ステップ3: サービス層のTDD

### 期待されるコード

#### `LoanService.java`（変更後の全体）

```java
package com.example.library.application;

import com.example.library.domain.LendingPolicy;
import com.example.library.domain.LoanExtensionPort;
import com.example.library.domain.RentalFeeCalculator;
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
    private final LoanExtensionPort loanExtensionPort;      // ← 追加

    public LoanService(BookRepository bookRepository,
                       MemberRepository memberRepository,
                       LoanRepository loanRepository,
                       LoanExtensionPort loanExtensionPort) {  // ← 追加
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
        this.loanExtensionPort = loanExtensionPort;           // ← 追加
    }

    public Loan borrowBook(String memberId, Long bookId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません: ID=" + memberId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: ID=" + bookId));

        if (!book.isAvailable()) {
            throw new IllegalStateException("この書籍は現在貸出中です");
        }

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
        int rentalFee = RentalFeeCalculator.calculateFee(
                member.getMemberType(),
                LendingPolicy.getLoanPeriodDays(member.getMemberType()),
                book.isNewRelease());
        loan.setRentalFee(rentalFee);
        return loanRepository.save(loan);
    }

    public Loan returnBook(Long loanId) {
        return returnBook(loanId, false);
    }

    public Loan returnBook(Long loanId, boolean isBookPostReturn) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("貸出記録が見つかりません: ID=" + loanId));

        Book book = bookRepository.findById(loan.getBookId())
                .orElseThrow(() -> new IllegalStateException("書籍が見つかりません: ID=" + loan.getBookId()));

        LocalDate returnDate = LocalDate.now();
        if (isBookPostReturn) {
            returnDate = returnDate.minusDays(1);
        }

        loan.returnBook(returnDate);
        book.returnBook();

        bookRepository.save(book);
        return loanRepository.save(loan);
    }

    // ── ここから変更 ──────────────────────────────────────

    public Loan extendLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("貸出記録が見つかりません: ID=" + loanId));

        if (!loan.isActive()) {
            throw new IllegalStateException("この貸出は既に返却済みです");
        }
        if (loan.isExtended()) {
            throw new IllegalStateException("この貸出は既に延長済みです");
        }

        loan.setDueDate(loan.getDueDate().plusDays(7));
        loan.setExtended(true);

        loanExtensionPort.saveExtension(loanId, loan.getDueDate());

        return loanRepository.save(loan);
    }

    // ── ここまで変更 ──────────────────────────────────────

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
import com.example.library.domain.FakeLoanExtensionPort;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoanServiceTest {

    // ── ここから追加 ──────────────────────────────────────

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public FakeLoanExtensionPort loanExtensionPort() {
            return new FakeLoanExtensionPort();
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
    FakeLoanExtensionPort fakeLoanExtensionPort;               // ← 追加

    private Member generalMember;
    private Book book;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();
        fakeLoanExtensionPort.reset();                         // ← 追加
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
                () -> loanService.borrowBook("9999999", book.getId()));
    }

    @Test
    void 存在しない書籍を貸出すると例外() {
        assertThrows(IllegalArgumentException.class,
                () -> loanService.borrowBook(generalMember.getId(), 9999L));
    }

    // ── ここから追加 ──────────────────────────────────────

    @Test
    void 貸出中の書籍を延長できる() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());
        LocalDate originalDueDate = loan.getDueDate();

        Loan extended = loanService.extendLoan(loan.getId());

        assertEquals(originalDueDate.plusDays(7), extended.getDueDate());
        assertTrue(extended.isExtended());
        assertTrue(extended.isActive());
    }

    @Test
    void 延長済みの貸出を再延長すると例外() {
        Loan loan = loanService.borrowBook(generalMember.getId(), book.getId());
        loanService.extendLoan(loan.getId());

        assertThrows(IllegalStateException.class,
                () -> loanService.extendLoan(loan.getId()));
    }

    // ── ここまで追加 ──────────────────────────────────────
}
```

### テスト実行結果

```
cd backend
mvn test -Dtest="*LoanServiceTest"
```

```
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 指導ポイント

- **`LoanService` のコンストラクタに `LoanExtensionPort` を追加すると既存テストが壊れる:**
  これは想定通り。Spring が `LoanExtensionPort` のBeanを見つけられずにエラーになる。`@TestConfiguration` で `FakeLoanExtensionPort` をBeanとして登録することで解決する。参加者がここで詰まった場合は、「Spring は `LoanService` を作るとき、コンストラクタの引数をすべてBeanとして見つける必要がある」と説明する。

- **`extendLoan()` のスタブについて:**
  現在の `extendLoan()` は `UnsupportedOperationException` をスローするスタブ状態になっている。参加者はこのスタブを実装に置き換える。スタブ内で `loan` 変数が使われずに例外をスローしている点は、SonarQube で未使用変数として指摘される可能性がある。

- **サービスがエンティティを変更してから保存することの説明:**
  `extendLoan()` では、サービスが `loan.setDueDate()` と `loan.setExtended(true)` でエンティティの状態を変更し、`loanRepository.save(loan)` でDBに保存する。ポート（`loanExtensionPort.saveExtension()`）はその**後**に呼ばれ、追加のDB操作を行う。この設計により、テスト時にフェイク（何もしないポート）を使っても、サービスの `loanRepository.save()` でDBが正しく更新される。

- **`borrowBook()` は変更しないことの説明:**
  `borrowBook()` メソッドには一切変更を加えない。変更するのは `extendLoan()` メソッドのみ。`LoanExtensionPort` はコンストラクタで注入されるが、`borrowBook()` や `returnBook()` からは使わない。

- **`@Primary` の役割について質問があった場合:**
  ステップ4で `DatabaseLoanExtensionAdapter`（`@Component`）を追加すると、`LoanExtensionPort` のBeanが2つ存在することになる。`@Primary` を付けると、テスト時にはフェイクが優先される。ステップ3時点では `DatabaseLoanExtensionAdapter` がまだ存在しないため `@Primary` は不要だが、先に付けておくことでステップ4でのトラブルを防げる。

---

## ステップ4: E2Eテスト

### 期待されるコード

#### `LoanExtensionPlaywrightTest.java`（新規 ─ コピー＆ペースト）

```java
package com.example.library.resource;

import com.example.library.PlaywrightTestBase;
import com.example.library.domain.LoanExtensionPort;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Import(LoanExtensionPlaywrightTest.TestConfig.class)
class LoanExtensionPlaywrightTest extends PlaywrightTestBase {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public LoanExtensionPort loanExtensionPort() {
            return (loanId, newDueDate) -> {};
        }
    }

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void setUpData() {
        restTemplate.postForEntity("/api/members",
                Map.of("id", "0000001", "name", "テスト会員",
                        "email", "test@example.com", "memberType", "GENERAL"),
                Map.class);
        restTemplate.postForEntity("/api/books",
                Map.of("title", "テスト書籍", "author", "テスト著者",
                        "isbn", "ISBN-001"),
                Map.class);
        restTemplate.postForEntity("/api/loans",
                Map.of("memberId", "0000001", "bookId", 1),
                Map.class);
    }

    @Test
    void 貸出中の書籍を延長できる() {
        navigateTo("/loans");

        page.getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions()
                        .setName("延長")).click();

        assertThat(page.locator("table").first()).containsText("延長済");
    }

    @Test
    void 延長後は延長ボタンが非表示になり延長済バッジが表示される() {
        navigateTo("/loans");

        page.getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions()
                        .setName("延長")).click();

        assertThat(page.locator("table").first()).containsText("延長済");
        assertThat(page.getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions()
                        .setName("延長"))).not().isVisible();
    }
}
```

#### `DatabaseLoanExtensionAdapter.java`（新規）

```java
package com.example.library.infrastructure;

import com.example.library.domain.LoanExtensionPort;
import com.example.library.domain.model.Loan;
import com.example.library.domain.repository.LoanRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DatabaseLoanExtensionAdapter implements LoanExtensionPort {

    private final LoanRepository loanRepository;

    public DatabaseLoanExtensionAdapter(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public void saveExtension(Long loanId, LocalDate newDueDate) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("貸出記録が見つかりません: ID=" + loanId));
        loan.setDueDate(newDueDate);
        loan.setExtended(true);
        loanRepository.save(loan);
    }
}
```

### テスト実行結果

```
# ユニットテスト + 統合テスト
mvn test
```

```
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```
# Playwrightテストを含む全テスト
mvn test -Dsurefire.excludedGroups=""
```

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

（テスト件数は既存テストの数により変動）

### 指導ポイント

- **Playwright テストはコピー＆ペーストであることの説明:**
  E2Eテストコードは参加者が自分で書くのではなく、課題ドキュメントからコピー＆ペーストで作成する。Playwright の詳細な使い方を覚えることがこの課題の主目的ではなく、テストピラミッドの中でE2Eテストがどのような位置づけにあるかを理解してもらうのが狙い。

- **Playwright テストのフェイクが `(loanId, newDueDate) -> {}` と何もしないことについて:**
  サービスの `extendLoan()` がエンティティの変更と `loanRepository.save()` を行うため、ポートが何もしなくてもDBは正しく更新される。フェイクの役割は「本物のアダプターの副作用を防ぐ」こと。

- **`@TestConfiguration` と `@Primary` の組み合わせ:**
  `DatabaseLoanExtensionAdapter`（`@Component`）と `@TestConfiguration` で定義したラムダBeanの2つが `LoanExtensionPort` を実装している。`@Primary` によりテスト時はフェイクが優先される。

- **`DatabaseLoanExtensionAdapter` が `LoanRepository` を使うことについて:**
  アダプターは `LoanRepository` を使って `loans` テーブルの `due_date` と `extended` を更新する。サービスも `loanRepository.save()` で同じ更新を行っている。JPA の永続化コンテキスト内では同じエンティティが管理されるため、実行時に競合は発生しない。

- **E2Eテストで既存テストが壊れた場合:**
  `DatabaseLoanExtensionAdapter` を追加すると、`LoanExtensionPort` のBeanが2つ存在することになる。テストクラスに `@TestConfiguration` + `@Primary` でフェイクを登録していないテストクラスでは、どちらのBeanを使うか解決できずにエラーになる。壊れた場合はそのテストクラスにも `@TestConfiguration` を追加するか、共通のテスト用設定クラスを `src/test/java` に切り出すことを検討する。

---

## ステップ5: SonarQube静的解析

### 想定される主な指摘事項

| 種別 | 指摘内容 | 該当箇所 | 対応例 |
|------|---------|---------|--------|
| Code Smell | 未使用変数 `loan` | `LoanService.extendLoan()` のスタブ（変更前） | ステップ3でスタブを実装に置き換えることで解消される |
| Coverage | `DatabaseLoanExtensionAdapter` のカバレッジ不足 | `infrastructure/` | E2Eテストではフェイクに差し替えているため、本物のDB更新コードはカバーされない。これは意図的 |

### 指導ポイント

- **カバレッジの解釈:**
  `DatabaseLoanExtensionAdapter` のカバレッジが低いのは、テストでフェイクに差し替えているため。これは正しいテスト設計の結果であり、カバレッジのためにテストでアダプターを使うべきではない。SonarQube の数値を盲信せず、テスト設計の意図を理解することが重要。

- **スタブの未使用変数について:**
  変更前の `extendLoan()` スタブでは `loan` 変数を取得した直後に `UnsupportedOperationException` をスローしており、SonarQube が未使用変数として指摘する可能性がある。ステップ3の実装で `loan` 変数を使うようになるため、この指摘は自然に解消される。参加者がステップ5の時点でこの指摘を見た場合は、「すでに解消済み」と確認してもらう。

---

## トラブルシューティング

### よくある質問と対応

| 症状 | 原因 | 対応 |
|------|------|------|
| `LoanService` のコンストラクタでエラー | `LoanExtensionPort` のBeanが見つからない | `@TestConfiguration` でフェイクをBean登録する |
| `extendLoan()` が `UnsupportedOperationException` をスローする | スタブを実装に置き換えていない | ステップ3で `extendLoan()` の中身を実装する |
| 延長済みテストが通ってしまう | `isExtended()` のチェックを入れていない | `loan.isExtended()` をチェックして `IllegalStateException` をスローする |
| E2Eテストで既存テストが壊れる | `DatabaseLoanExtensionAdapter` がBean登録されてBeanが2つ存在する | `@TestConfiguration` + `@Primary` でフェイクを優先させる |
| `FakeLoanExtensionPort` を `@Autowired` できない | `@Bean` の戻り値型が `LoanExtensionPort` になっている | 戻り値型を `FakeLoanExtensionPort` に変更する |
| Playwright テストで「延長」ボタンが見つからない | フロントエンドの実装が未反映 | フロントエンドに延長ボタンと延長済バッジのUIが実装されていることを確認する |
| `infrastructure` パッケージが見つからない | ディレクトリが未作成 | `src/main/java/com/example/library/infrastructure/` を手動で作成する |
