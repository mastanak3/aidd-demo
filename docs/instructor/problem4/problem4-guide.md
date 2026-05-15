# 課題4: 蔵書検索機能の追加 - 講師ガイド

## ゴール

ローカルでは成功するがCI環境（並列実行）では失敗するテストを体験し、テストケースの独立性（F.I.R.S.T原則のI）の重要性を学ぶ。

## 構成（65分）

1. **ステップ1**: AIを使ったPBI実装（20分）- 蔵書検索機能の実装
2. **ステップ2**: ローカルテストの確認（10分）- `mvn test` で全テスト成功
3. **ステップ3**: CI環境でのテスト実行・調査（20分）- `mvn test -Pci` で失敗 → 原因調査 → 修正
4. **ステップ4**: 振り返り（15分）

## 仕込まれたトラップの仕組み

### トラップの場所

`backend/src/test/java/com/example/library/application/LoanLifecycleIntegrationTest.java`

### トラップの内容

このテストクラスには以下の反パターンが意図的に組み込まれています:

1. **テスト間のデータ共有（`static`フィールド）**
   ```java
   private static Long memberId;
   private static Long bookId;
   private static Long loanId;
   ```
   テストメソッド間で状態をstatic変数で共有している。

2. **テストの実行順序への依存（`@Order`）**
   ```java
   @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
   // ...
   @Test @Order(1) void 書籍を貸出できる() { ... loanId = loan.getId(); }
   @Test @Order(2) void 貸出中の書籍は利用不可になる() { ... bookService.findById(bookId); }
   @Test @Order(3) void 書籍を返却できる() { ... loanService.returnBook(loanId); }
   @Test @Order(4) void 返却後は書籍が利用可能になる() { ... bookService.findById(bookId); }
   ```
   各テストが前のテストの副作用に依存している。

3. **不適切なSetup（lazy initialization）**
   ```java
   @BeforeEach
   void setUp() {
       if (memberId == null) {  // 最初のテストでのみデータ作成
           dbCleaner.cleanAll();
           // ... データ作成 ...
       }
   }
   ```
   `@BeforeEach`で毎回データを再作成せず、最初の1回だけ作成する。後続テストではデータの再作成が行われない。

### なぜローカルでは成功するか

ローカル環境（`mvn test`、`forkCount=1`）では:
- テストクラスが1つずつ順番に実行される
- `LoanLifecycleIntegrationTest`内のメソッドは`@Order`順に実行される
- `@Order(1)`でデータ作成 → `@Order(2)`で状態確認 → `@Order(3)`で返却 → `@Order(4)`で確認
- 他のテストクラスのcleanAll()は、このクラスの全テスト完了後に実行される
- 結果: **全テスト成功**

### なぜCIでは失敗するか

CI環境（`mvn test -Pci`、`forkCount=4` + JUnit 5並列実行）では:

**原因1: クロスフォーク干渉（forkCount=4）**
- 4つのJVMプロセスが同じPostgreSQLデータベースを共有
- Fork 1で`LoanLifecycleIntegrationTest`のデータ作成
- Fork 2の`BookServiceTest.@BeforeEach`で`cleanAll()`（TRUNCATE TABLE）が実行される
- Fork 1のデータが消去 → static変数の参照先IDが存在しなくなる
- `書籍が見つかりません: ID=X` のようなエラーが発生

**原因2: メソッド並列実行（JUnit 5 parallel）**
- `@Order`順にメソッドが開始されるが、並列実行のため完了を待たずに次が開始
- `@Order(3)`の`returnBook(loanId)`が実行される時点で`loanId`がまだnull
- NullPointerExceptionまたは不正な状態でのエラー

### 想定されるエラーメッセージ

```
LoanLifecycleIntegrationTest.貸出中の書籍は利用不可になる -- ERROR!
  java.lang.IllegalArgumentException: 書籍が見つかりません: ID=7

LoanLifecycleIntegrationTest.書籍を返却できる -- ERROR!
  java.lang.NullPointerException: Cannot invoke ... because "loanId" is null

MemberServiceTest.会員を更新できる -- ERROR!
  java.lang.IllegalArgumentException: 会員が見つかりません: ID=1

LoanServiceTest.一般会員は3冊まで借りられる -- ERROR!
  java.lang.IllegalArgumentException: 会員が見つかりません: ID=2
```

> **注意:** 並列実行はタイミングに依存するため、毎回同じテストが失敗するとは限りません。ただし、`LoanLifecycleIntegrationTest`は高い確率で失敗します。

### 他テストクラスの巻き添え失敗について

`LoanLifecycleIntegrationTest`以外のテスト（`MemberServiceTest`、`LoanServiceTest`等）も失敗することがあります。これは`cleanAll()`（TRUNCATE TABLE）が全フォークで共有されるデータベースに対して実行されるためです。

**参加者にはまず`LoanLifecycleIntegrationTest`の問題に注目してもらってください。** このテストクラスが最も明確な反パターンを持っており、修正の方法もわかりやすいです。

## 正しい修正方法

### 修正前（反パターン）

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoanLifecycleIntegrationTest {
    private static Long memberId;  // static共有
    private static Long bookId;
    private static Long loanId;

    @BeforeEach
    void setUp() {
        if (memberId == null) {  // 一度だけ初期化
            dbCleaner.cleanAll();
            // ... 省略 ...
        }
    }

    @Test @Order(1)
    void 書籍を貸出できる() { ... loanId = loan.getId(); }

    @Test @Order(2)
    void 貸出中の書籍は利用不可になる() { ... bookService.findById(bookId); }
}
```

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
        assertEquals(member.getId(), loan.getMemberId());
        assertEquals(book.getId(), loan.getBookId());
        assertTrue(loan.isActive());
    }

    @Test
    void 貸出中の書籍は利用不可になる() {
        // このテスト内で貸出を行ってから確認
        loanService.borrowBook(member.getId(), book.getId());

        Book found = bookService.findById(book.getId());
        assertFalse(found.isAvailable());
    }

    @Test
    void 書籍を返却できる() {
        // このテスト内で貸出→返却
        Loan loan = loanService.borrowBook(member.getId(), book.getId());
        Loan returned = loanService.returnBook(loan.getId());

        assertNotNull(returned.getReturnDate());
        assertFalse(returned.isActive());
    }

    @Test
    void 返却後は書籍が利用可能になる() {
        // このテスト内で貸出→返却→確認
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
| ステップ2完了後 | 「全テスト通りましたね！ではCI環境で試してみましょう」 |
| ステップ3-1実行後 | 「ローカルとCI環境で結果が違いますね。何が違うのでしょう？」（間を取る） |
| エラー確認時 | 「エラーメッセージを読んでみましょう。何が起きていますか？」 |
| CI設定確認時 | 「pom.xmlのciプロファイルを見てみましょう。何が変わっていますか？」 |
| テストコード確認時 | 「このテストクラスの`static`フィールドと`@Order`に注目してください」 |
| 修正完了後 | 「修正前と修正後、テストコードの構造がどう変わりましたか？」 |

### トラブルシューティング

**CI環境でのテストが成功してしまう場合**

並列実行はタイミングに依存するため、稀に成功することがあります。
→ 再度 `mvn test -Pci` を実行してください。複数回実行するとほぼ確実に失敗します。

**参加者がトラップに気づけない場合**

ヒントを段階的に提供:
1. 「`LoanLifecycleIntegrationTest.java`のコードを見てみましょう」
2. 「`static`キーワードに注目してください。なぜstaticなのでしょう？」
3. 「`@BeforeEach`で毎回データを作っていますか？`if (memberId == null)`は何をしていますか？」
4. 「テスト1の結果をテスト2が使っています。これは並列実行でどうなりますか？」

**修正が不完全な場合**

よくある不完全な修正:
- `static`を外したがlazyinitはそのまま → `@BeforeEach`で毎回cleanAll()が必要
- `@Order`を外したがテスト間の依存はそのまま → 各テストを自己完結型にする必要あり
- `cleanAll()`を追加したが`@Order`はそのまま → 順序依存を完全に排除する必要あり

### 振り返りのポイント

1. **テストの独立性（F.I.R.S.T原則のI: Independent）**
   - 各テストは他のテストの実行結果に依存してはならない
   - テストの実行順序に依存してはならない
   - 並列実行でもシリアル実行でも同じ結果が得られるべき

2. **Setup/Teardownの重要性**
   - `@BeforeEach`で毎回テストデータを初期化する
   - テストが終わったら状態をクリーンにする
   - lazy initializationは並列実行で危険

3. **ローカル環境の罠**
   - ローカルでの成功はCI環境での成功を保証しない
   - テスト実行環境の違い（並列度、タイミング）を意識する
   - 「自分の環境では動く」は品質保証にならない

## キーメッセージ

**テストの独立性（F.I.R.S.T原則のI）**
- 各テストは独立して実行可能でなければならない
- テスト間でstatic変数やグローバル状態を共有しない
- `@BeforeEach`で毎回テストの前提条件を構築する

**Setup/Teardownの徹底**
- テストデータは毎回クリーン＆再作成
- lazy initializationではなく、毎回の初期化
- 「1回作って使い回す」は効率的に見えて危険

**CI環境を前提としたテスト設計**
- ローカルでの成功に安心しない
- 並列実行を前提としたテスト設計
- AI駆動開発でも、テストの独立性は開発者が担保する責任がある

**AI駆動開発における開発者の役割**
- AIはテストを生成できるが、テストの独立性までは保証しない
- CI環境での実行を想定したテスト設計は開発者のスキル
- 「動くテスト」と「信頼できるテスト」の違いを理解することが重要
