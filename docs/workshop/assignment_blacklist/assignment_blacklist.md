# 課題: 会員ブラックリスト照会機能 ─ TDDで作る外部サービス連携

## 課題概要

| 項目 | 内容 |
|------|------|
| 所要時間 | 約2時間30分（設計15分 + ドメインTDD30分 + サービスTDD30分 + E2Eテスト30分 + SonarQube20分 + CI統合15分 + 振り返り10分） |
| 前提知識 | Java / Spring Boot の基礎、JUnit 5 の基本的な使い方 |

---

## PBI

### 会員ブラックリスト照会機能

**ユーザーストーリー:**

> 図書館の職員として、書籍の貸出処理を行う際に、会員がブラックリストに登録されていないかを確認したい。そうすることで、問題のある会員への貸出を未然に防げる。

**背景:**

図書館では、度重なる延滞や不正利用などの理由でブラックリストに登録された会員への貸出を制限する必要があります。ブラックリスト情報は外部の会員管理サービスで一元管理されており、貸出時にそのサービスへ照会を行います。

**Acceptance Criteria:**

| # | 条件 | 詳細 |
|---|------|------|
| AC-1 | ブラックリスト照会 | 会員IDで外部ブラックリストサービスに照会できる |
| AC-2 | 貸出時の自動チェック | 書籍の貸出処理時にブラックリストを自動的に確認する |
| AC-3 | ブラックリスト該当時の拒否 | ブラックリストに該当する会員の貸出をエラーとして拒否する |

---

## 外部ブラックリストサービス仕様

本課題では、別途提供される外部ブラックリストサービスを利用します。

**サービスURL:** 当日講師からお伝えします。

### API仕様

#### ブラックリスト照会

```
GET /api/blacklist/check?memberId={会員ID}
```

**リクエスト例:**

```
GET /api/blacklist/check?memberId=0000001
```

**該当あり（200 OK）:**

```json
{
  "memberId": "0000001",
  "blacklisted": true,
  "reason": "延滞金未納"
}
```

**該当なし（200 OK）:**

```json
{
  "memberId": "0000002",
  "blacklisted": false,
  "reason": null
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
| 運用 | CI/CD | パイプラインへのテスト・解析の統合 |

**重要:** 本課題のコードはすべて手作業で記述します。

---

## 手順

### ステップ1: 設計（15分）

#### 1-1. PBIの理解

PBIの Acceptance Criteria と外部サービス仕様を読み、以下を考えてください。

> **問いかけ:**
> - この機能は既存のどのメソッドに影響しますか？（ヒント: `LoanService` の貸出処理）

#### 1-2. どんなクラスを作るか考える

この機能を実装するために、いくつかの新しいクラスが必要です。
ここでのポイントは、**外部サービスを直接呼び出すコードと、業務ロジックを分離する**ことです。

そのために**インターフェース**を使います。業務ロジック側はインターフェースだけを知っていればよく、実際にHTTP通信する実装クラスの中身を知る必要はありません。こうすると、テスト時に「偽物の実装」に差し替えることが簡単にできます。

**作成するクラスの全体像:**

```
domain/
├── model/
│   └── BlacklistStatus.java      ← 照会結果を入れるデータクラス（新規）
└── BlacklistCheckPort.java        ← 外部サービスを呼ぶためのインターフェース（新規）

※ 会員IDは7桁の文字列型です（例: "0000001"）

infrastructure/                    ← 新しいパッケージ
└── ExternalBlacklistAdapter.java  ← インターフェースの実装（実際にHTTP通信する）（新規）

application/
└── LoanService.java              ← ブラックリストチェックを組み込む（変更）
```

> **なぜこのような構成にするのか？**
>
> インターフェース（`BlacklistCheckPort`）を `domain/` に置き、HTTP通信の実装（`ExternalBlacklistAdapter`）を `infrastructure/` に置くことで、業務ロジックが通信の詳細に依存しなくなります。
> テスト時には、このインターフェースの「偽物」を作ってHTTP通信なしでテストできます。

> **チェックポイント:**
> - `BlacklistStatus` にはどんなフィールドが必要そうですか？
>   （ヒント: 外部サービスのレスポンス JSON を見てみましょう）
> - `BlacklistCheckPort` にはどんなメソッドが1つあればよいですか？
>   引数は何（会員ID）で、戻り値は何にしますか？

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

**1. `FakeBlacklistCheckPort`**（`src/test/java` に配置）

`BlacklistCheckPort` インターフェースのテスト用の偽実装です。
テストケースごとに振る舞いを変えられるように作ります。

> **ヒント:** こんな機能を持たせると便利です
> - コンストラクタやセッターで「返すべき `BlacklistStatus`」を設定できる

**2. `BlacklistCheckPortTest`**（テストクラス）

以下の2つのテストケースを書いてください:

| # | テストケース | 期待結果 |
|---|------------|---------|
| 1 | ブラックリスト該当の会員IDで照会する | `blacklisted` が `true` の `BlacklistStatus` が返る |
| 2 | ブラックリスト非該当の会員IDで照会する | `blacklisted` が `false` の `BlacklistStatus` が返る |

> **ヒント:** テストのイメージ
> ```java
> // 1. フェイクを用意して「該当あり」を返すように設定
> // 2. フェイクの check メソッドを呼ぶ
> // 3. 結果が「該当あり」であることを検証
> ```

テストを実行してみましょう（コンパイルエラーまたは失敗になるはずです）:

```bash
mvn test -pl backend -Dtest="*BlacklistCheckPortTest"
```

> **確認ポイント:**
> - コンパイルエラーまたはテスト失敗になりましたか？ → それが **Red** の状態です

#### 2-2. Green: テストが通る最小限のコードを書く

テストを通すために、プロダクションコードを作成します。

**作成するファイル（2つ）:**

**1. `BlacklistStatus.java`**（`domain/model/` に配置）

照会結果を表すデータクラスです。Java の `record` を使うとシンプルに書けます。

> **ヒント:** 必要なフィールド
> - `memberId` ─ 照会した会員ID（7桁の文字列）
> - `blacklisted` ─ ブラックリストに該当するかどうか（`boolean`）
> - `reason` ─ 該当理由（該当なしの場合は `null`）

**2. `BlacklistCheckPort.java`**（`domain/` に配置）

外部サービスを呼ぶためのインターフェースです。メソッドは1つだけで十分です。

> **ヒント:** メソッドのイメージ
> - メソッド名: `check`
> - 引数: `String memberId`
> - 戻り値: `BlacklistStatus`

テストを実行して、パス（Green）することを確認します:

```bash
mvn test -pl backend -Dtest="*BlacklistCheckPortTest"
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
mvn test -pl backend -Dtest="*BlacklistCheckPortTest"
```

---

### ステップ3: サービス層のTDD（30分）

ドメイン層ができたので、`LoanService` にブラックリストチェックを組み込みます。
ここでもTDDの Red → Green → Refactor で進めます。

#### 3-1. Red: テストを先に書く

既存の `LoanServiceTest` に、ブラックリスト関連のテストケースを**追加**します。

**追加するテストケース:**

| # | テストケース | 期待結果 |
|---|------------|---------|
| 1 | ブラックリスト非該当の会員が書籍を借りる | 正常に貸出できる |
| 2 | ブラックリスト該当の会員が書籍を借りる | `IllegalStateException` がスローされる |

テスト内では、ステップ2で作成した `FakeBlacklistCheckPort` を使ってください。

> **ヒント:** テストのイメージ（テストケース2の場合）
> ```java
> // 1. テスト用の会員と書籍をDBに登録する
> // 2. FakeBlacklistCheckPort を「該当あり」を返すように設定する
> // 3. loanService.borrowBook(...) を呼ぶ
> // 4. IllegalStateException がスローされることを検証する
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
> `LoanService` に `BlacklistCheckPort` を注入するようにすると、既存のテストにも影響が出ます。
> 既存テストにも `FakeBlacklistCheckPort`（「該当なし」を返す設定）を渡すように修正してください。

#### 3-2. Green: LoanServiceを変更する

`LoanService.borrowBook()` にブラックリストチェックを追加します。

**やること:**

1. `LoanService` のコンストラクタに `BlacklistCheckPort` を追加する（依存注入）
2. `borrowBook()` の中で `BlacklistCheckPort.check()` を呼び出す
3. ブラックリスト該当なら `IllegalStateException` をスローする

> **ヒント:** 実装イメージ
> ```java
> BlacklistStatus status = blacklistCheckPort.check(member.getId());
> if (status.blacklisted()) {
>     throw new IllegalStateException("...");
> }
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
> - `borrowBook()` メソッドが長くなりすぎていませんか？
>   ブラックリストチェックの部分を別の `private` メソッドに切り出すと読みやすくなるかもしれません
> - リファクタリング後もテストが通ることを確認してください

```bash
mvn test -pl backend
```

---

### ステップ4: E2Eテスト（30分）

Playwright を使って、**ブラウザ経由**でブラックリスト機能をテストします。
このステップでは、テストコードは**コピー＆ペースト**で作成します。
合わせて、本番用のHTTP通信の実装も作成します。

#### 4-1. Playwright テストの作成

以下のファイルを **新規作成** し、コードをそのままコピー＆ペーストしてください。

**ファイル:** `src/test/java/com/example/library/resource/BlacklistPlaywrightTest.java`

```java
package com.example.library.resource;

import com.example.library.PlaywrightTestBase;
import com.example.library.domain.BlacklistCheckPort;
import com.example.library.domain.model.BlacklistStatus;
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

@Import(BlacklistPlaywrightTest.TestConfig.class)
class BlacklistPlaywrightTest extends PlaywrightTestBase {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public BlacklistCheckPort blacklistCheckPort() {
            // 会員ID "0000002" のみブラックリスト該当として扱う
            return memberId -> new BlacklistStatus(memberId,
                    "0000002".equals(memberId),
                    "0000002".equals(memberId) ? "延滞金未納" : null);
        }
    }

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void setUpData() {
        restTemplate.postForEntity("/api/members",
                Map.of("id", "0000001", "name", "通常会員",
                        "email", "normal@example.com", "memberType", "GENERAL"),
                Map.class);
        restTemplate.postForEntity("/api/members",
                Map.of("id", "0000002", "name", "ブラックリスト会員",
                        "email", "blocked@example.com", "memberType", "GENERAL"),
                Map.class);
        restTemplate.postForEntity("/api/books",
                Map.of("title", "テスト書籍", "author", "テスト著者",
                        "isbn", "ISBN-001"),
                Map.class);
    }

    @Test
    void ブラックリスト非該当の会員は貸出できる() {
        navigateTo("/loans");

        page.locator("select").first().selectOption(
                new com.microsoft.playwright.options.SelectOption()
                        .setLabel("通常会員"));
        page.locator("select").nth(1).selectOption(
                new com.microsoft.playwright.options.SelectOption()
                        .setLabel("テスト書籍"));
        page.getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions()
                        .setName("貸出")).click();

        assertThat(page.locator("table").first()).containsText("テスト書籍");
        assertThat(page.locator("table").first()).containsText("通常会員");
    }

    @Test
    void ブラックリスト該当の会員は貸出が拒否される() {
        navigateTo("/loans");

        page.locator("select").first().selectOption(
                new com.microsoft.playwright.options.SelectOption()
                        .setLabel("ブラックリスト会員"));
        page.locator("select").nth(1).selectOption(
                new com.microsoft.playwright.options.SelectOption()
                        .setLabel("テスト書籍"));
        page.getByRole(AriaRole.BUTTON,
                new com.microsoft.playwright.Page.GetByRoleOptions()
                        .setName("貸出")).click();

        // エラーメッセージが表示されることを検証
        assertThat(page.locator(".error")).isVisible();
        // 貸出中一覧に追加されていないことを検証
        assertThat(page.getByText("貸出中の書籍はありません")).isVisible();
    }
}
```

> **コードの解説:**
> - `@TestConfiguration` + `@Primary` で、テスト実行時だけ `BlacklistCheckPort` のBeanを差し替えています
> - 会員ID `"0000002"` の場合のみ `blacklisted: true` を返すようにしています
> - `@BeforeEach` で REST API 経由でテストデータ（会員2名・書籍1冊）を登録しています
> - テスト自体は、ブラウザで貸出ページを操作して結果を検証しています

テストを実行してみましょう:

```bash
mvn test -pl backend -Dsurefire.excludedGroups="" -Dtest="*BlacklistPlaywrightTest"
```

> **確認ポイント:**
> - テストがパスしましたか？
> - 失敗した場合は、ステップ3までの実装に問題がないか確認してください

#### 4-2. HTTP通信の実装（本番用）

テストではフェイクを使いましたが、本番環境では実際に外部サービスへHTTPリクエストを送る必要があります。
そのための実装を作成します。

**作成するファイル:**

**`ExternalBlacklistAdapter.java`**（`infrastructure/` パッケージに配置）

`BlacklistCheckPort` インターフェースの「本物の」実装です。
実際に外部サービスにHTTPリクエストを送信します。

> **ヒント:**
> - Spring の `RestClient` または `RestTemplate` を使ってHTTP通信を実装します
> - `@Component` を付けてSpring Beanとして登録します
> - サービスURLは `application.properties` から読み込みます

**設定の追加:**

`application.properties` にブラックリストサービスのURLを追加してください:

```
blacklist.service.url=${BLACKLIST_SERVICE_URL:http://localhost:8081}
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

### ステップ6: CIパイプライン統合（15分）

ここまでの作業（ビルド・テスト・静的解析）をCIパイプラインで自動化します。
コードをプッシュするたびに、これらが自動実行されるようになります。

#### 6-1. ワークフローファイルの作成

以下のファイルを新規作成してください。

**ファイル:** `.gitea/workflows/ci.yaml`

**パイプラインに含めるステージ:**

```
[コード取得] → [ビルド] → [ユニットテスト] → [E2Eテスト] → [SonarQube解析]
```

| ステージ | 実行するコマンド | 備考 |
|---------|----------------|------|
| コード取得 | `actions/checkout` | |
| ビルド | `mvn compile -pl backend` | |
| ユニットテスト | `mvn test -pl backend` | E2Eテスト除外（デフォルト動作） |
| E2Eテスト | `mvn test -pl backend -Dsurefire.excludedGroups=""` | 全テスト実行 |
| SonarQube解析 | `mvn sonar:sonar -pl backend` | 静的解析 |

> **ヒント:**
> - Gitea Actions は GitHub Actions とほぼ同じ書式です
> - テストにはPostgreSQLが必要です。`services:` でデータベースコンテナを追加してください
> - SonarQubeのURL・トークンは `secrets` として設定します
> - 行き詰まったら、GitHub Actions の書き方を参考にしてみてください

#### 6-2. パイプラインの実行と検証

変更をコミット・プッシュして、CIパイプラインの実行結果を確認します。

```bash
git add .
git commit -m "feat: add blacklist check with CI pipeline"
git push
```

Gitea のリポジトリページで **Actions** タブを開いて、パイプラインの実行結果を確認してください。

> **チェックポイント:**
> - パイプラインは正常に完了しましたか？
> - 失敗したステージがある場合は、ログを読んで原因を特定してください
> - よくある失敗原因: DB接続エラー、SonarQubeのURL設定ミス、環境変数の未設定

---

### ステップ7: 振り返り（10分）

以下の問いについて、グループで議論してください。

#### TDDについて

1. **テストを先に書いてみて、どうでしたか？**
   - テストを先に書くことで、実装が楽になった部分はありましたか？
   - 「テストが通る最小限のコード」を意識できましたか？
   - Refactor フェーズで何を改善しましたか？

#### SonarQubeとCI/CDについて

2. **静的解析は役に立ちましたか？**
   - SonarQubeの指摘で気づいた改善点はありましたか？
   - 人間のレビューとの違いは何ですか？

3. **CI/CDの意味**
   - テスト・解析をCIで自動化すると、何がうれしいですか？
   - 「手元でテストが通ればいい」ではなぜ不十分なのですか？
