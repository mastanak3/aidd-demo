# 課題: 貸出延長機能 ─ TDDで作る外部サービス連携

## 課題概要

| 項目 | 内容 |
|------|------|
| 所要時間 | 約2時間（設計15分 + ドメインTDD30分 + サービスTDD30分 + E2Eテスト30分 + SonarQube20分 + 振り返り10分） |
| 前提知識 | Java / Spring Boot の基礎、JUnit 5 の基本的な使い方 |

---

## PBI

### 貸出延長機能

**ユーザーストーリー:**

> 図書館の職員として、会員からの貸出延長依頼を処理する際に、延長を外部の延長管理サービスに登録したい。そうすることで、延長記録が一元管理される。

**背景:**

図書館では、会員が貸出中の書籍を1回だけ延長できるようになっています。延長依頼は外部の延長管理サービスに登録する必要があります。システムは延長が可能かどうかを検証してから登録を行います。

**Acceptance Criteria:**

| # | 条件 | 詳細 |
|---|------|------|
| AC-1 | 貸出延長 | 貸出中の書籍の返却期限を7日間延長できる |
| AC-2 | 外部登録 | 延長を外部の延長管理サービスにPOSTで登録する |
| AC-3 | 延長済みの拒否 | 既に延長済みの貸出は再延長できない |

---

## 外部延長管理サービス仕様

本課題では、別途提供される外部延長管理サービスを利用します。

**サービスURL:** 当日講師からお伝えします。

### API仕様

#### 延長登録

```
POST /api/extensions
```

**リクエスト例:**

```json
{
  "memberId": "0000001",
  "loanId": 1,
  "newDueDate": "2025-02-01"
}
```

**登録成功（201 Created）:**

```json
{
  "extensionId": "EXT-12345",
  "status": "REGISTERED"
}
```

**サービス障害時（503 Service Unavailable）:**

```json
{
  "error": "Service Unavailable"
}
```

---

## 本課題で学ぶこと

この課題では、1つの機能をゼロから完成させるまでの**ソフトウェア開発の一連の流れ**を体験します。

| ステップ | 学習テーマ | やること |
|---------|-----------|--------|
| 設計 | 役割ごとにクラスを分ける | インターフェースで外部サービスとの接点を切り離す |
| 実装 | テスト駆動開発（TDD） | Red → Green → Refactor サイクル |
| テスト | テストダブルの使い分け | 手書きのフェイク（偽実装）で外部サービスを置き換える |
| 品質 | 静的解析 | SonarQubeによるコード品質の可視化と改善 |

**重要:** 本課題のコードはすべて手作業で記述します。

---

## 手順

### ステップ1: 設計（15分）

#### 1-1. PBIの理解

PBIの Acceptance Criteria と外部サービス仕様を読み、以下を考えてください。

> **問いかけ:**
> - この機能は既存のどのメソッドに影響しますか？（ヒント: `LoanService` の `extendLoan()` メソッド）
> - 現在 `extendLoan()` はどうなっていますか？（ヒント: `UnsupportedOperationException` をスローしている）

#### 1-2. どんなクラスを作るか考える

この機能を実装するために、いくつかの新しいクラスが必要です。
ここでのポイントは、**外部サービスを直接呼び出すコードと、業務ロジックを分離する**ことです。

そのために**インターフェース**を使います。業務ロジック側はインターフェースだけを知っていればよく、実際にHTTP通信する実装クラスの中身を知る必要はありません。こうすると、テスト時に「偽物の実装」に差し替えることが簡単にできます。

**作成するクラスの全体像:**

```
domain/
├── model/
│   └── ExtensionResult.java      ← 外部サービスの応答を入れるデータクラス（新規）
└── LoanExtensionPort.java        ← 外部サービスを呼ぶためのインターフェース（新規）

※ 会員IDは7桁の文字列型です（例: "0000001"）
※ 貸出IDはLong型です
※ Loan エンティティには extended（boolean）フィールドが既に存在しています

infrastructure/
└── ExternalLoanExtensionAdapter.java  ← インターフェースの実装（実際にHTTP通信する）（新規）

application/
└── LoanService.java              ← extendLoan() のスタブを実装に置き換え（変更）
```

> **なぜこのような構成にするのか？**
>
> インターフェース（`LoanExtensionPort`）を `domain/` に置き、HTTP通信の実装（`ExternalLoanExtensionAdapter`）を `infrastructure/` に置くことで、業務ロジックが通信の詳細に依存しなくなります。
> テスト時には、このインターフェースの「偽物」を作ってHTTP通信なしでテストできます。

> **補足: 既存コードの確認**
>
> `LoanController` にはすでに `POST /api/loans/{id}/extend` エンドポイントが用意されており、`loanService.extendLoan(id)` を呼び出しています。現在の `extendLoan()` は `UnsupportedOperationException` をスローするスタブ状態です。

> **チェックポイント:**
> - `ExtensionResult` にはどんなフィールドが必要そうですか？
>   （ヒント: 外部サービスのレスポンス JSON を見てみましょう）
> - `LoanExtensionPort` にはどんなメソッドが1つあればよいですか？
>   引数は何（会員ID、貸出ID、新しい返却期限）で、戻り値は何にしますか？

#### 1-3. テストの方針

本課題では、テストで使う依存先の扱いを以下のように**固定**します。

| 依存先 | 例 | テストでの扱い |
|-------|-----|-------------|
| DB（リポジトリ） | `LoanRepository`, `BookRepository` | **本物**をそのまま使う |
| 外部サービス | 延長管理サービス | **フェイク（手書きの偽実装）**に置き換える |

DBは自分たちが管理しているものなので、テストでも本物を使います。
一方、外部の延長管理サービスはテスト中に呼び出せないため、**フェイク**に差し替えます。

> **フェイクとは？**
>
> インターフェースを `implements` した**普通のJavaクラス**です。
> テスト側で「登録成功」といった振る舞いを
> 自由に設定できるように作ります。`src/test/java` に配置します。

---

### ステップ2: ドメイン層のTDD（30分）

TDDでは **Red → Green → Refactor** のサイクルで開発を進めます。

- **Red**: テストを先に書く → テストは失敗する（まだ実装がないので当然）
- **Green**: テストが通る最小限のプロダクションコードを書く
- **Refactor**: テストが通る状態を維持しながらコードを整理する

#### 2-1. Red: テストを先に書く

まず**テストコード**から作成します。
この時点ではプロダクションコードが存在しないため、コンパイルエラーになりますが、**それが正常です。**

**作成するファイル（2つ）:**

**1. `FakeLoanExtensionPort`**（`src/test/java` に配置）

`LoanExtensionPort` インターフェースのテスト用の偽実装です。
テストケースごとに振る舞いを変えられるように作ります。

> **ヒント:** こんな機能を持たせると便利です
> - コンストラクタやセッターで「返すべき `ExtensionResult`」を設定できる

**2. `LoanExtensionPortTest`**（テストクラス）

以下のテストケースを書いてください:

| # | テストケース | 期待結果 |
|---|------------|---------|
| 1 | 延長を外部サービスに登録する | `ExtensionResult` が返る（`extensionId`, `status`） |

> **ヒント:** テストのイメージ
> ```java
> // 1. フェイクを用意して ExtensionResult を返すように設定
> // 2. フェイクの register メソッドを呼ぶ
> // 3. 結果の extensionId と status を検証する
> ```

テストを実行してみましょう（コンパイルエラーまたは失敗になるはずです）:

```bash
mvn test -pl backend -Dtest="*LoanExtensionPortTest"
```

> **確認ポイント:**
> - コンパイルエラーまたはテスト失敗になりましたか？ → それが **Red** の状態です

#### 2-2. Green: テストが通る最小限のコードを書く

テストを通すために、プロダクションコードを作成します。

**作成するファイル（2つ）:**

**1. `ExtensionResult.java`**（`domain/model/` に配置）

外部サービスの応答を表すデータクラスです。Java の `record` を使うとシンプルに書けます。

> **ヒント:** 必要なフィールド
> - `extensionId` ─ 外部サービスが発行した延長ID（`String`）
> - `status` ─ 登録結果のステータス（`String`）

**2. `LoanExtensionPort.java`**（`domain/` に配置）

外部サービスを呼ぶためのインターフェースです。メソッドは1つだけで十分です。

> **ヒント:** メソッドのイメージ
> - メソッド名: `register`
> - 引数: `String memberId, Long loanId, LocalDate newDueDate`
> - 戻り値: `ExtensionResult`

テストを実行して、パス（Green）することを確認します:

```bash
mvn test -pl backend -Dtest="*LoanExtensionPortTest"
```

> **チェックポイント:**
> - すべてのテストがパスしましたか？
> - 余計なフィールドやメソッドを追加していませんか？（今必要なものだけ作る）

#### 2-3. Refactor: コードを整理する

テストが通る状態を維持しながら、コードを読みやすくします。

> **確認すること:**
> - クラス名やメソッド名は、やっていることを正確に表していますか？
> - リファクタリング後にテストを再実行して、引き続きパスすることを確認しましょう

```bash
mvn test -pl backend -Dtest="*LoanExtensionPortTest"
```

---

### ステップ3: サービス層のTDD（30分）

ドメイン層ができたので、`LoanService` の `extendLoan()` を実装します。
ここでもTDDの Red → Green → Refactor で進めます。

#### 3-1. Red: テストを先に書く

既存の `LoanServiceTest` に、貸出延長関連のテストケースを**追加**します。

**追加するテストケース:**

| # | テストケース | 期待結果 |
|---|------------|---------|
| 1 | 貸出中の書籍を延長する | 返却期限が7日延長される |
| 2 | 延長済みの貸出を再延長する | `IllegalStateException` がスローされる |

テスト内では、ステップ2で作成した `FakeLoanExtensionPort` を使ってください。

> **ヒント:** テストのイメージ（テストケース1の場合）
> ```java
> // 1. テスト用の会員と書籍をDBに登録し、貸出する
> // 2. FakeLoanExtensionPort を用意する
> // 3. loanService.extendLoan(loanId) を呼ぶ
> // 4. 返却期限が7日延長されていることを検証する
> ```

テストを実行してみましょう:

```bash
mvn test -pl backend -Dtest="*LoanServiceTest"
```

> **確認ポイント:**
> - 新しいテストが失敗（Red）しましたか？
> - 既存のテストはどうなりましたか？
>
> **既存テストが壊れた場合:**
>
> `LoanService` に `LoanExtensionPort` を注入するようにすると、既存のテストにも影響が出ます。
> 既存テストにも `FakeLoanExtensionPort` を渡すように修正してください。
> 既存のメソッド（`borrowBook`, `returnBook`）は延長ポートを使わないので、何もしないフェイクで十分です。

#### 3-2. Green: LoanServiceを変更する

`LoanService.extendLoan()` のスタブを実装に置き換えます。

**やること:**

1. `LoanService` のコンストラクタに `LoanExtensionPort` を追加する（依存注入）
2. `extendLoan()` の中で以下の処理を実装する:
   - 貸出を取得する（既にスタブにある）
   - 貸出中（未返却）であることを確認する
   - 延長済みでないことを確認する
   - 返却期限を7日延長し、延長済みフラグを立てる
   - 会員を取得する
   - `loanExtensionPort.register(...)` で外部サービスに登録する
   - 保存して返す

> **ヒント:** 実装イメージ
> ```java
> if (!loan.isActive()) {
>     throw new IllegalStateException("この貸出は既に返却済みです");
> }
> if (loan.isExtended()) {
>     throw new IllegalStateException("この貸出は既に延長済みです");
> }
> loan.setDueDate(loan.getDueDate().plusDays(7));
> loan.setExtended(true);
>
> Member member = memberRepository.findById(loan.getMemberId())
>         .orElseThrow(...);
> loanExtensionPort.register(member.getId(), loanId, loan.getDueDate());
> ```

テストを実行します:

```bash
mvn test -pl backend -Dtest="*LoanServiceTest"
```

> **チェックポイント:**
> - 新規テスト2つと既存テストの両方がパスしましたか？

#### 3-3. Refactor

コードを整理しましょう。

> **確認すること:**
> - `extendLoan()` メソッドが長くなりすぎていませんか？
>   バリデーション部分を別の `private` メソッドに切り出すと読みやすくなるかもしれません
> - リファクタリング後もテストが通ることを確認してください

```bash
mvn test -pl backend
```

---

### ステップ4: E2Eテスト（30分）

Playwright を使って、**ブラウザ経由**で貸出延長機能をテストします。
このステップでは、テストコードは**コピー＆ペースト**で作成します。
合わせて、本番用のHTTP通信の実装も作成します。

#### 4-1. Playwright テストの作成

以下のファイルを **新規作成** し、コードをそのままコピー＆ペーストしてください。

**ファイル:** `src/test/java/com/example/library/resource/LoanExtensionPlaywrightTest.java`

```java
package com.example.library.resource;

import com.example.library.PlaywrightTestBase;
import com.example.library.domain.LoanExtensionPort;
import com.example.library.domain.model.ExtensionResult;
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
            return (memberId, loanId, newDueDate) ->
                    new ExtensionResult("EXT-TEST-001", "REGISTERED");
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

> **コードの解説:**
> - `@TestConfiguration` + `@Primary` で、テスト実行時だけ `LoanExtensionPort` のBeanを差し替えています
> - フェイクの `LoanExtensionPort` は常に成功レスポンス（`EXT-TEST-001`, `REGISTERED`）を返します
> - `@BeforeEach` で REST API 経由でテストデータ（会員1名・書籍1冊・貸出1件）を登録しています
> - テスト1は「延長」ボタンをクリックして「延長済」バッジが表示されることを検証しています
> - テスト2は延長後に「延長」ボタンが消え、「延長済」バッジが表示されることを検証しています

テストを実行してみましょう:

```bash
mvn test -pl backend -Dsurefire.excludedGroups="" -Dtest="*LoanExtensionPlaywrightTest"
```

> **確認ポイント:**
> - テストがパスしましたか？
> - 失敗した場合は、ステップ3までの実装に問題がないか確認してください

#### 4-2. HTTP通信の実装（本番用）

テストではフェイクを使いましたが、本番環境では実際に外部サービスへHTTPリクエストを送る必要があります。
そのための実装を作成します。

**作成するファイル:**

**`ExternalLoanExtensionAdapter.java`**（`infrastructure/` パッケージに配置）

`LoanExtensionPort` インターフェースの「本物の」実装です。
実際に外部サービスにHTTPリクエストを送信します。

> **ヒント:**
> - Spring の `RestClient` または `RestTemplate` を使ってHTTP通信を実装します
> - `@Component` を付けてSpring Beanとして登録します
> - サービスURLは `application.properties` から読み込みます
> - リクエストボディには `memberId`, `loanId`, `newDueDate` を含めます
> - レスポンスから `extensionId` と `status` を取得して `ExtensionResult` を返します

**設定の追加:**

`application.properties` に延長管理サービスのURLを追加してください:

```
extension.service.url=${EXTENSION_SERVICE_URL:http://localhost:8081}
```

#### 4-3. 全テスト実行

すべてのテストを通して実行し、全体が正しく動くことを確認します。

```bash
# ユニットテスト + 統合テスト
mvn test -pl backend

# Playwrightテストを含む全テスト
mvn test -pl backend -Dsurefire.excludedGroups=""
```

> **チェックポイント:**
> - すべてのテスト（ユニット、統合、Playwright）がパスしましたか？
> - カバレッジレポートを生成して、新しく作ったクラスのカバレッジを確認してください
> ```bash
> mvn test -pl backend jacoco:report
> ```
> - `backend/target/site/jacoco/index.html` をブラウザで開いて確認できます

---

### ステップ5: SonarQube静的解析とリファクタリング（20分）

#### 5-1. SonarQube解析の実行

SonarQube で静的解析を実行します。静的解析とは、コードを実行せずにソースコードを解析して、潜在的な問題やコード品質の改善点を自動的に検出するものです。

```bash
mvn sonar:sonar -pl backend \
  -Dsonar.projectKey=library \
  -Dsonar.host.url=${SONAR_HOST_URL} \
  -Dsonar.token=${SONAR_TOKEN}
```

> **注:** SonarQubeのURL・トークンは当日講師からお伝えします。

#### 5-2. 解析結果の確認

SonarQubeのダッシュボードをブラウザで開いて、結果を見てみましょう。

> **確認するポイント:**
> - **Code Smells（コードの臭い）**: 「こう書いたほうが良い」と提案されているコードはありますか？
> - **Coverage（カバレッジ）**: テストでカバーできていない行はどこですか？
> - **Bugs**: バグの可能性がある箇所はありますか？
> - **Duplications（重複）**: 同じようなコードが複数箇所にありませんか？

#### 5-3. リファクタリング

SonarQubeの指摘から改善すべき項目を**1〜2個**選んで、修正してみましょう。

**リファクタリングの進め方:**

1. 指摘を1つ選ぶ
2. 指摘の内容を読んで、なぜ問題なのか理解する
3. コードを修正する
4. テストを再実行して、壊れていないことを確認する

```bash
mvn test -pl backend
```

> **チェックポイント:**
> - リファクタリング後もテストがすべてパスしましたか？
> - テストが壊れた場合は、リファクタリングが振る舞いを変えてしまっている可能性があります。
>   リファクタリングとは「振る舞いを変えずに構造を改善する」ことです

#### 5-4. 再解析

修正後、SonarQubeを再実行して指摘が減ったことを確認してください。

```bash
mvn sonar:sonar -pl backend \
  -Dsonar.projectKey=library \
  -Dsonar.host.url=${SONAR_HOST_URL} \
  -Dsonar.token=${SONAR_TOKEN}
```

---

### ステップ6: 振り返り（10分）

以下の問いについて、グループで議論してください。

#### TDDについて

1. **テストを先に書いてみて、どうでしたか？**
   - テストを先に書くことで、実装が楽になった部分はありましたか？
   - 「テストが通る最小限のコード」を意識できましたか？
   - Refactor フェーズで何を改善しましたか？

#### SonarQubeについて

2. **静的解析は役に立ちましたか？**
   - SonarQubeの指摘で気づいた改善点はありましたか？
   - 人間のレビューとの違いは何ですか？
