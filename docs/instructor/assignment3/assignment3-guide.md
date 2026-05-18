# 課題3: 外部サービス連携とISBN自動検索 - 講師ガイド

## ゴール

外部サービスに依存するテストの不安定さ（Flaky Test）を体験し、「管理下にない依存」をテストダブルに置き換える原則（FIRST原則のR: Repeatable）を学ぶ。

## 想定される展開

1. **実装完了** → 既存テストパス → 安心感
2. **外部サービスを呼ぶテスト作成** → 一見パスするが、潜在的リスクに気づく（または不安定さを体験する）
3. **テストダブルで解決** → 決定的なテストの価値を実感 → 学び

## 各ステップの講師ポイント

### ステップ1: 機能の実装（10分）

- インターフェース（`IsbnLookupService`）と`BookService`の変更は完成コードとして提供済み。受講者はコードを読んで理解し、ファイルを作成・編集する
- **穴埋め対象は`ExternalIsbnLookupService`のみ**。TODO 1〜3を実装させる
- 既存テスト（`BookServiceTest`、`LoanServiceTest`）のWeldInitiatorへの`ExternalIsbnLookupService.class`追加も手順に含まれている

**受講者が詰まりやすいポイント:**

| 問題 | フォロー |
|------|----------|
| `URLEncoder.encode()`の使い方がわからない | `URLEncoder.encode(title, StandardCharsets.UTF_8)` と伝える |
| `HttpClient`の使い方がわからない | 以下のコード例を見せる（下記参照） |
| JSON解析の方法がわからない | 簡易的には`body.split("\"isbn\":\"")[1].split("\"")[0]`で抽出できると伝える。余裕があれば`com.fasterxml.jackson.databind.ObjectMapper`の使用を案内 |
| `try-catch`でチェック例外が処理できない | `HttpClient.send()`は`IOException`と`InterruptedException`をスローする。`try-catch`で囲み`RuntimeException`に変換するよう案内 |
| ディレクトリ`infrastructure/service/`が存在しない | 新規作成が必要。パスを確認するよう案内 |

**HttpClientのコード例（行き詰まった場合に見せる）:**

```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .GET()
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

### ステップ2: テストの作成（10分）

- テストクラスのスケルトン（imports、WeldSetup、@BeforeEach）は提供済み。受講者はテストメソッド3つを記述する
- **重要:** このステップでは外部サービスを実際に呼び出すテストを意図的に書かせる。問題はステップ3で顕在化する
- `isbn-service-booklist.md`を参照して、実在する書籍名とISBNを使うよう案内する

**テストメソッドの書き方で詰まった場合:**
- 「`bookService.create("Clean Code", "Robert C. Martin", null)` で書籍を作成し、`assertEquals("978-0132350884", book.getIsbn())` で検証する」と具体例を見せる

### ステップ3: テストの実行（10分）

課題文書では全員に5回連続実行させた上で、結果に関わらず以下を問いかける構成にしている。

**テストが不安定になった場合：**
- 「なぜ結果がばらつくのか？」をそのまま次ステップの分析につなげる

**テストが安定してパスしてしまう場合（こちらが多い可能性あり）：**
- 課題文書の問いかけ（「いつどこでも同じ結果と言い切れるか？」「外部サービスが落ちたらどうなるか？」）を活用して、リスクの存在に気づかせる
- 「今たまたまパスしているだけで、CI環境や深夜のバッチ実行では同じ結果になりますか？」と補足してもよい

### ステップ4: 原因分析と対策（20分）

ここが本課題の核心。テストが実際に壊れたかどうかに関わらず、外部依存のリスクとその対策に焦点を当てる。段階的に問いかける：

1. 「このテストの成否を決めているのは、テストコードですか？外部の何かですか？」
2. 「`IsbnLookupService`はインターフェースですね。テスト専用の実装は作れませんか？」
3. 「成功を返す実装と、必ず例外を投げる実装があれば、両方のケースをテストできませんか？」

**想定される解法:** テスト用フェイク実装（Stub）を作り、WeldInitiatorで差し替える。成功ケースと失敗ケースでテストクラスを分ける。

> LoanServiceTestのWeldInitiatorにも`ExternalIsbnLookupService`が含まれている場合、同様にStubに差し替えが必要。

## トラブルシューティング

| 問題 | 対応 |
|------|------|
| コンパイルエラー（import漏れ） | エラーメッセージを読んで不足しているimport文を追加するよう案内 |
| パッケージ・ディレクトリの構造ミス | `infrastructure/service/`ディレクトリの作成忘れが多い。パスを確認 |
| WeldInitiatorの設定漏れ | `BookService`が`IsbnLookupService`をInjectするため、WeldInitiatorに実装クラスが必要と説明 |
| `HttpClient.send()`のチェック例外未処理 | `try-catch`で`IOException`と`InterruptedException`を捕捉し、`RuntimeException`でラップ |
| JSON解析で文字列操作に失敗 | レスポンスの具体例（`{"title":"Clean Code","isbn":"978-0132350884"}`）を見せて確認させる |

## キーメッセージ

- **テストの決定性は開発者が設計する。** 非決定的な要素を自律的に排除するのは開発者の責務
- **管理下にない依存はテストダブルに置き換える。** 管理下にある依存（DB等）は本物を使う
- **インターフェースがテスタビリティを生む。** 設計がテストの品質に直結する
