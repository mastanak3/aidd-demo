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

図書館では、延滞金未納や不正利用などの理由でブラックリストに登録された会員への貸出を制限する必要があります。ブラックリスト情報は外部の会員管理サービスで一元管理されており、貸出時にそのサービスへ照会を行います。

**Acceptance Criteria:**

| # | 条件 | 詳細 |
|---|------|------|
| AC-1 | ブラックリスト照会 | 会員のメールアドレスで外部ブラックリストサービスに照会できる |
| AC-2 | 貸出時の自動チェック | 書籍の貸出処理時にブラックリストを自動的に確認する |
| AC-3 | ブラックリスト該当時の拒否 | ブラックリストに該当する会員の貸出をエラーとして拒否する |
| AC-4 | サービス障害時のフォールバック | 外部サービスが利用不可の場合、貸出を許可する（フェイルオープン） |

---

## 外部ブラックリストサービス仕様

本課題では、別途提供される外部ブラックリストサービスを利用します。

**サービスURL:** 当日講師からお伝えします。

### API仕様

#### ブラックリスト照会

```
GET /api/blacklist/check?email={メールアドレス}
```

**リクエスト例:**

```
GET /api/blacklist/check?email=blocked@example.com
```

**該当あり（200 OK）:**

```json
{
  "email": "blocked@example.com",
  "blacklisted": true,
  "reason": "延滞金未納"
}
```

**該当なし（200 OK）:**

```json
{
  "email": "good@example.com",
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
> - 外部サービスが落ちたとき、貸出を止めるべきですか？それとも許可すべきですか？（AC-4 を確認）

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
>   引数は何で、戻り値は何にしますか？

#### 1-3. テストの方針を決める

外部サービスをテストでどう扱うか、方針を決めます。

本課題では**古典学派テスト**のアプローチを採用します。考え方はシンプルです:

- **自分たちが管理しているもの**（DB、リポジトリなど）→ テストでも**本物を使う**
- **自分たちが管理していないもの**（外部サービス）→ テストでは**偽物に差し替える**

| 依存先 | 例 | テストでの扱い |
|-------|-----|-------------|
| 自分たちのDB | `LoanRepository`, `BookRepository` | **本物**を使う |
| 外部サービス | ブラックリストサービス | **フェイク（手書きの偽実装）**に置き換える |

> **フェイクとは？**
>
> Mockito の `@Mock` とは違い、インターフェースを `implements` した**普通のJavaクラス**です。
> テスト側で「ブラックリストに該当する」「該当しない」「例外を投げる」といった振る舞いを
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

**1. `FakeBlacklistCheckPort`**（`src/test/java` に配置）

`BlacklistCheckPort` インターフェースのテスト用の偽実装です。
テストケースごとに振る舞いを変えられるように作ります。

> **ヒント:** こんな機能を持たせると便利です
> - コンストラクタやセッターで「返すべき `BlacklistStatus`」を設定できる
> - 「例外をスローするモード」にも切り替えられる（AC-4のテスト用）

**2. `BlacklistCheckPortTest`**（テストクラス）

以下の2つのテストケースを書いてください:

| # | テストケース | 期待結果 |
|---|------------|---------|
| 1 | ブラックリスト該当のメールアドレスで照会する | `blacklisted` が `true` の `BlacklistStatus` が返る |
| 2 | ブラックリスト非該当のメールアドレスで照会する | `blacklisted` が `false` の `BlacklistStatus` が返る |

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
> - `email` ─ 照会したメールアドレス
> - `blacklisted` ─ ブラックリストに該当するかどうか（`boolean`）
> - `reason` ─ 該当理由（該当なしの場合は `null`）

**2. `BlacklistCheckPort.java`**（`domain/` に配置）

外部サービスを呼ぶためのインターフェースです。メソッドは1つだけで十分です。

> **ヒント:** メソッドのイメージ
> - メソッド名: `check`
> - 引数: `String email`
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
| 3 | 外部サービスが障害で応答しない場合に書籍を借りる | 正常に貸出できる（AC-4: フェイルオープン） |

テスト内では、ステップ2で作成した `FakeBlacklistCheckPort` を使ってください。

> **ヒント:** テストのイメージ（テストケース2の場合）
> ```java
> // 1. テスト用の会員と書籍をDBに登録する
> // 2. FakeBlacklistCheckPort を「該当あり」を返すように設定する
> // 3. loanService.borrowBook(...) を呼ぶ
> // 4. IllegalStateException がスローされることを検証する
> ```

> **ヒント:** テストケース3（サービス障害）のイメージ
> ```java
> // 1. FakeBlacklistCheckPort を「例外をスローする」モードに設定する
> // 2. loanService.borrowBook(...) を呼ぶ
> // 3. 例外がスローされず、正常に貸出できることを検証する
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
4. 外部サービスの例外は `catch` して、貸出を許可する（フェイルオープン）

> **ヒント:** フェイルオープンの実装イメージ
> ```java
> try {
>     BlacklistStatus status = blacklistCheckPort.check(member.getEmail());
>     if (status.blacklisted()) {
>         throw new IllegalStateException("...");
>     }
> } catch (RuntimeException e) {
>     // 外部サービスの障害時は貸出を許可する（フェイルオープン）
> }
> ```
>
> ただし、上記のコードには**問題が1つ**あります。考えてみてください。
>
> （ヒント: `IllegalStateException` も `RuntimeException` の一種です）

テストを実行します:

```bash
mvn test -pl backend -Dtest="*LoanServiceTest"
```

> **チェックポイント:**
> - 新規テスト3つと既存テストの両方がパスしましたか？
> - AC-4（フェイルオープン）のテストもパスしていますか？

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

REST API 経由でのテスト（E2Eテスト）を追加し、HTTP通信の実装も作成します。

#### 4-1. Red: E2Eテストを書く

`LoanControllerTest`（`@Tag("e2e")`）にブラックリスト関連のテストケースを追加します。

**追加するテストケース:**

| # | シナリオ | HTTPリクエスト | 期待レスポンス |
|---|---------|-------------|-------------|
| 1 | ブラックリスト該当会員が貸出 | `POST /api/loans` | `409 Conflict` |
| 2 | ブラックリスト非該当会員が貸出 | `POST /api/loans` | `201 Created`（既存テストで担保済みかもしれません） |

**E2Eテストでの外部サービスの扱い:**

E2Eテストでも外部サービスは「偽物」に差し替えます。
Spring の `@TestConfiguration` を使うと、テスト実行時だけ特定のBeanを差し替えることができます。

> **ヒント:** `@TestConfiguration` の使い方
> ```java
> @TestConfiguration
> static class TestConfig {
>     @Bean
>     public BlacklistCheckPort blacklistCheckPort() {
>         // テスト用のフェイクを返す
>         return new FakeBlacklistCheckPort(...);
>     }
> }
> ```
>
> このクラスをテストクラスの中に書いて、`@Import(TestConfig.class)` でテストに読み込ませます。
>
> ただし、E2Eテストでは「該当あり」のケースと「該当なし」のケースの両方をテストする必要があります。
> テストメソッドごとにフェイクの振る舞いを切り替える方法を考えてみてください。

テストを実行してみましょう:

```bash
mvn test -pl backend -Dsurefire.excludedGroups="" -Dtest="*LoanControllerTest"
```

> **確認ポイント:**
> - テストが失敗（Red）しましたか？

#### 4-2. Green: HTTP通信の実装とSpring設定

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

E2Eテストを実行して、パスすることを確認します:

```bash
mvn test -pl backend -Dsurefire.excludedGroups="" -Dtest="*LoanControllerTest"
```

> **チェックポイント:**
> - E2Eテストがパスしましたか？
> - 既存のE2Eテストが壊れていませんか？
>   壊れた場合は、`@TestConfiguration` が既存テストにも影響していないか確認してください

#### 4-3. 全テスト実行

すべてのテストを通して実行し、全体が正しく動くことを確認します。

```bash
# ユニットテスト + 統合テスト
mvn test -pl backend

# E2Eテストを含む全テスト
mvn test -pl backend -Dsurefire.excludedGroups=""
```

> **チェックポイント:**
> - すべてのテスト（ユニット、統合、E2E）がパスしましたか？
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

#### インターフェースによる分離について

2. **インターフェースを使ってよかったことは？**
   - テスト時にフェイク（偽実装）に差し替えられましたか？
   - もしインターフェースを使わず `ExternalBlacklistAdapter`（HTTP通信するクラス）を
     直接 `LoanService` に書いていたら、テストはどうなっていたと思いますか？

3. **パッケージの分け方（domain / application / infrastructure）**
   - それぞれに何を置きましたか？
   - この分け方のメリットは感じられましたか？

#### テストダブルについて

4. **手書きのフェイク vs Mockito**
   - `FakeBlacklistCheckPort` を手書きした感想は？
   - Mockito の `@Mock` / `when(...).thenReturn(...)` と比べて、どちらが分かりやすかったですか？
   - それぞれのメリット・デメリットを挙げてみてください

5. **「本物を使うもの」と「偽物に置き換えるもの」の区別**
   - DB（リポジトリ）は本物を使い、外部サービスはフェイクに置き換えました。なぜですか？
   - もしDBもモックにしていたら、どんなテストになっていたと思いますか？

#### SonarQubeとCI/CDについて

6. **静的解析は役に立ちましたか？**
   - SonarQubeの指摘で気づいた改善点はありましたか？
   - 人間のレビューとの違いは何ですか？

7. **CI/CDの意味**
   - テスト・解析をCIで自動化すると、何がうれしいですか？
   - 「手元でテストが通ればいい」ではなぜ不十分なのですか？
