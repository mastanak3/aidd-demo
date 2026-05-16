# 課題4: 貸出更新機能の実装と統合テスト - 講師ガイド

## ゴール

PBI実装 → AIによるテスト生成という実践的な流れの中で、AIが生成した「効率的だが壊れやすい」テストを体験し、テストケースの独立性（F.I.R.S.T原則のI）の重要性を学ぶ。

## 構成（60分）

1. **ステップ1**: PBI実装（10分）- 貸出更新機能をAIで実装
2. **ステップ2**: テスト生成（10分）- AIで統合テストを生成
3. **ステップ3**: ローカルテストの確認（5分）- `mvn test` で全テスト成功
4. **ステップ4**: CI環境でのテスト実行・調査・修正（25分）- `mvn test -Pci` で失敗
5. **ステップ5**: 振り返り（10分）

## ステップ1: AIが実装する想定コード

### Loan.java への追加

```java
public void renew(LocalDate newDueDate) {
    if (returnDate != null) {
        throw new IllegalStateException("返却済みの貸出は更新できません");
    }
    this.dueDate = newDueDate;
}
```

### LoanService.java への追加

```java
public Loan renewLoan(Long loanId) {
    Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("貸出が見つかりません: ID=" + loanId));
    Member member = memberRepository.findById(loan.getMemberId())
            .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません: ID=" + loan.getMemberId()));
    LocalDate newDueDate = lendingPolicy.calculateDueDate(member.getMemberType(), LocalDate.now());
    loan.renew(newDueDate);
    return loanRepository.save(loan);
}
```

> **ポイント:** 実装自体はシンプルで、既存テストを壊さない。ステップ1は安心感を持って完了できる。

## ステップ2: AIが反パターンを生成する仕組み

### 誘導プロンプトのポイント

ワークショップドキュメントに記載されたプロンプトには、AIが反パターンを生成するよう誘導するキーワードが含まれています:

| プロンプトのキーワード | AIが生成するパターン | 問題点 |
|----------------------|-------------------|--------|
| 「**順序通りに**検証」 | `@TestMethodOrder` + `@Order` | テスト間の実行順序依存 |
| 「テストデータを**使い回して**」 | `static`フィールドで状態共有 | テスト間の状態依存 |
| 「**実行速度を最適化**」 | lazy init（初回のみデータ作成） | `@BeforeEach`で毎回初期化しない |
| 「**1つのテストクラス**で」 | 単一クラスに依存テストを集約 | テストの独立性欠如 |

### AIが生成する想定コード

```java
@EnableWeld
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanLifecycleIntegrationTest {

    // ... WeldSetup, Inject省略 ...

    private static Long memberId;    // ← static共有
    private static Long bookId;
    private static Long loanId;
    private static LocalDate originalDueDate;

    @BeforeEach
    void setUp() {
        if (memberId == null) {       // ← 初回のみ初期化
            dbCleaner.cleanAll();
            Member member = memberService.create(...);
            memberId = member.getId();
            Book book = bookService.create(...);
            bookId = book.getId();
        }
    }

    @Test @Order(1)
    void 書籍を貸出できる() {
        Loan loan = loanService.borrowBook(memberId, bookId);
        // ... assertions ...
        loanId = loan.getId();              // ← 後続テストのためにstatic保存
        originalDueDate = loan.getDueDate(); // ← 後続テストのためにstatic保存
    }

    @Test @Order(2)
    void 貸出を更新して返却期限が延長される() {
        Loan renewed = loanService.renewLoan(loanId);  // ← @Order(1)で設定されたloanIdに依存
        assertTrue(renewed.getDueDate().isAfter(originalDueDate));  // ← @Order(1)で設定された値に依存
    }

    @Test @Order(3)
    void 更新後も書籍は貸出不可のまま() {
        Book found = bookService.findById(bookId);  // ← @Order(1)の副作用に依存
        assertFalse(found.isAvailable());
    }

    @Test @Order(4)
    void 書籍を返却して状態が復帰する() {
        Loan returned = loanService.returnBook(loanId);  // ← @Order(1)で設定されたloanIdに依存
        Book found = bookService.findById(bookId);
        assertTrue(found.isAvailable());
    }
}
```

### なぜローカルでは成功するか

ローカル環境（`mvn test`、`forkCount=1`）では:
- テストクラスが1つずつ順番に実行される
- メソッドは`@Order`順に実行される
- `@Order(1)`で貸出 → `@Order(2)`で更新 → `@Order(3)`で状態確認 → `@Order(4)`で返却
- 他のテストクラスの`cleanAll()`は、このクラスの全テスト完了後に実行される
- 結果: **全テスト成功**

### なぜCIでは失敗するか

CI環境（`mvn test -Pci`、`forkCount=4` + JUnit 5並列実行）では:

**原因1: クロスフォーク干渉（forkCount=4）**
- 4つのJVMプロセスが同じPostgreSQLデータベースを共有
- 別フォークの`BookServiceTest.@BeforeEach`で`cleanAll()`（TRUNCATE TABLE）が実行される
- ライフサイクルテストのデータが消去 → static変数の参照先IDが存在しなくなる

**原因2: メソッド並列実行（JUnit 5 parallel）**
- `@Order`順にメソッドが開始されるが、並列実行のため完了を待たずに次が開始
- `@Order(2)`の`renewLoan(loanId)`が実行される時点で`loanId`がまだnull

### 想定されるエラーメッセージ

```
LoanLifecycleIntegrationTest.貸出を更新して返却期限が延長される -- ERROR!
  java.lang.NullPointerException: Cannot invoke ... because "loanId" is null

LoanLifecycleIntegrationTest.更新後も書籍は貸出不可のまま -- ERROR!
  java.lang.IllegalArgumentException: 書籍が見つかりません: ID=X

LoanLifecycleIntegrationTest.書籍を返却して状態が復帰する -- ERROR!
  java.lang.NullPointerException: Cannot invoke ... because "loanId" is null
```

> **注意:** 並列実行はタイミングに依存するため、毎回同じテストが失敗するとは限りません。他のテストクラス（`MemberServiceTest`等）も巻き添えで失敗する場合があります。

## 正しい修正方法

### 修正後（正しいパターン）

```java
// @TestMethodOrder を削除
class LoanLifecycleIntegrationTest {
    // static を削除 → インスタンスフィールドに
    private Member member;
    private Book book;

    @BeforeEach
    void setUp() {
        // 毎回クリーン＆再作成
        dbCleaner.cleanAll();
        member = memberService.create("統合テスト会員", "lifecycle@example.com", MemberType.GENERAL);
        book = bookService.create("統合テスト書籍", "テスト著者", "ISBN-LIFECYCLE");
    }

    @Test
    void 書籍を貸出できる() {
        Loan loan = loanService.borrowBook(member.getId(), book.getId());
        assertNotNull(loan.getId());
        assertTrue(loan.isActive());
    }

    @Test
    void 貸出を更新して返却期限が延長される() {
        Loan loan = loanService.borrowBook(member.getId(), book.getId());
        LocalDate originalDueDate = loan.getDueDate();
        Loan renewed = loanService.renewLoan(loan.getId());
        assertTrue(renewed.getDueDate().isAfter(originalDueDate));
        assertTrue(renewed.isActive());
    }

    @Test
    void 更新後も書籍は貸出不可のまま() {
        Loan loan = loanService.borrowBook(member.getId(), book.getId());
        loanService.renewLoan(loan.getId());
        Book found = bookService.findById(book.getId());
        assertFalse(found.isAvailable());
    }

    @Test
    void 書籍を返却して状態が復帰する() {
        Loan loan = loanService.borrowBook(member.getId(), book.getId());
        loanService.returnBook(loan.getId());
        Book found = bookService.findById(book.getId());
        assertTrue(found.isAvailable());
    }
}
```

### 修正のポイント

| 修正箇所 | 修正前 | 修正後 | 理由 |
|----------|--------|--------|------|
| フィールド | `private static Long` | `private Member/Book`（インスタンス） | static共有を排除 |
| @BeforeEach | 初回のみ初期化（lazy init） | 毎回cleanAll()＋再作成 | テスト間の独立性確保 |
| @TestMethodOrder | あり（@Order指定） | 削除 | 実行順序への依存を排除 |
| テストメソッド | 前のテストの副作用に依存 | 各テスト内で必要な前提条件を構築 | 自己完結型テスト |

## ファシリテーションガイド

### 各ステップでの問いかけ

| タイミング | 問いかけ |
|-----------|---------|
| ステップ1完了後 | 「PBI実装は簡単でしたね。既存テストも通っています。次はテストを書きましょう」 |
| ステップ2完了後 | 「AIが生成したテストコードを見てみましょう。何か気になる点はありますか？」 |
| ステップ3完了後 | 「全テスト通りましたね！このテストは信頼できると思いますか？」 |
| ステップ4-1実行後 | 「ローカルとCI環境で結果が違いますね。何が違うのでしょう？」（間を取る） |
| エラー確認時 | 「エラーメッセージを読んでみましょう。何が起きていますか？」 |
| CI設定確認時 | 「pom.xmlのciプロファイルを見てみましょう。何が変わっていますか？」 |
| テストコード再確認 | 「自分が生成したテストの`static`フィールドと`@Order`を見てください。並列実行ではどうなりますか？」 |
| 修正完了後 | 「修正前と修正後、テストコードの構造がどう変わりましたか？」 |

### トラブルシューティング

**PBI実装でAIが既存テストを壊した場合**

AIがLoan.javaの既存メソッドを変更してしまうことがあります。

対処法:
1. `git diff`で変更内容を確認し、既存メソッドへの変更がないか確認
2. AIに「既存のメソッドは変更せず、新しいメソッドのみ追加してください」と再指示

**AIが正しい独立テストを生成した場合**

稀にAIが`@BeforeEach`で毎回データを作成する正しいパターンを生成することがあります。

対処法（段階的に試す）:
1. 「テストデータの作成が毎回行われていて遅いです。初期化を1回だけにして実行速度を改善してください」と追加指示
2. 「`@Order`を使ってテストの実行順序を制御し、テストデータの作成を最初のテストでのみ行うようにリファクタリングしてください」と追加指示
3. 上記でも正しいテストが出る場合、以下のサンプルコードを参加者に提供:

```java
// このコードをLoanLifecycleIntegrationTest.javaとして保存してください
// （インストラクターガイドの「AIが生成する想定コード」セクションを参照）
```

**CI環境でのテストが成功してしまう場合**

並列実行はタイミングに依存するため、稀に成功することがあります。
→ 再度 `mvn test -Pci` を実行してください。複数回実行するとほぼ確実に失敗します。

**修正が不完全な場合**

よくある不完全な修正:
- `static`を外したがlazy initはそのまま → `@BeforeEach`で毎回cleanAll()が必要
- `@Order`を外したがテスト間の依存はそのまま → 各テストを自己完結型にする必要あり
- `cleanAll()`を追加したが`@Order`はそのまま → 順序依存を完全に排除する必要あり

### 振り返りのポイント

1. **PBI実装 → テスト生成の流れ**
   - プロダクトコード実装はAIが適切に対応できた
   - しかしテスト生成では、プロンプト次第で品質が大きく変わる
   - 実装と同じAIに依頼しても、テスト設計の品質は保証されない

2. **AIの生成結果を鵜呑みにしない**
   - AIはプロンプトの指示に忠実に従う
   - 「効率化」を求めると、テストの品質を犠牲にしたコードを生成する
   - 生成されたコードの品質を評価するのは開発者の責任

3. **テストの独立性（F.I.R.S.T原則のI: Independent）**
   - 各テストは他のテストの実行結果に依存してはならない
   - テストの実行順序に依存してはならない
   - 並列実行でもシリアル実行でも同じ結果が得られるべき

4. **Setup/Teardownの重要性**
   - `@BeforeEach`で毎回テストデータを初期化する
   - lazy initializationは並列実行で危険
   - 「効率化」より「信頼性」を優先する

## キーメッセージ

**PBI実装とテスト生成は別の課題**
- AIはプロダクトコードの実装を適切に行えることが多い
- しかしテスト生成では、テスト設計の原則を理解していないと品質の評価ができない
- プロンプトの書き方次第で、反パターンを忠実に実装する

**AIが生成したテストの落とし穴**
- AIは「動くテスト」を書けるが、「信頼できるテスト」を書くとは限らない
- プロンプトの指示次第で、反パターンを忠実に実装する
- テストの品質評価は開発者が担う責任

**テストの独立性（F.I.R.S.T原則のI）**
- 各テストは独立して実行可能でなければならない
- テスト間で`static`変数やグローバル状態を共有しない
- `@BeforeEach`で毎回テストの前提条件を構築する

**CI環境を前提としたテスト設計**
- ローカルでの成功はCI環境での成功を保証しない
- 並列実行を前提としたテスト設計
- 「自分の環境では動く」は品質保証にならない
