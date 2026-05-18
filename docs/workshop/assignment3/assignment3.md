# 課題3: 外部サービス連携とISBN自動検索

## 課題概要

| 項目 | 内容 |
|------|------|
| 所要時間 | 約50分 |
| 前提知識 | ベースラインプロジェクトでの開発経験 |

---

## PBI

### ISBN自動検索機能の追加

**ユーザーストーリー:**

> 図書館の職員として、書籍を登録する際に本の名前を入力するだけでISBNが自動入力されるようにしたい。そうすることで、ISBNを調べる手間が省ける。

**背景:**

書籍を登録する際、ISBNを毎回手動で調べて入力するのは手間がかかります。外部のISBN検索サービスを利用して、書名からISBNを自動取得する機能を追加します。

**Acceptance Criteria:**

| # | 条件 | 詳細 |
|---|------|------|
| AC-1 | ISBN省略可能 | 書籍登録時にISBNを省略（null or 空文字）できる |
| AC-2 | ISBN自動取得 | ISBNが省略された場合、書名で外部サービスからISBNを自動取得する |
| AC-3 | エラー時のフォールバック | 外部サービスが失敗した場合、ISBNは`"UNKNOWN"`として登録する |

---

## 外部ISBN検索サービス仕様

本課題では、別途提供されるISBN検索サービスを利用します。

**サービスURL:** 当日講師からお伝えします。

サービスが検索可能な書籍の一覧は [isbn-service-booklist.md](isbn-service-booklist.md) を参照してください。

### API仕様

#### ISBN検索

```
GET /api/isbn-lookup?title={書名}
```

**リクエスト例:**

```
GET /api/isbn-lookup?title=Clean%20Code
```

**成功時レスポンス（200 OK）:**

```json
{
  "title": "Clean Code",
  "isbn": "978-0132350884"
}
```

**該当なしレスポンス（404 Not Found）:**

```json
{
  "error": "Book not found"
}
```

**エラー時レスポンス（503 Service Unavailable）:**

```json
{
  "error": "Service Unavailable"
}
```

---

## 手順

### ステップ1: 機能の実装（10分）

以下のコードをプロジェクトに追加して、ISBN自動検索機能を実装します。

#### 1-1. インターフェースの作成

以下のファイルを作成してください。

**`backend/src/main/java/com/example/library/application/IsbnLookupService.java`**

```java
package com.example.library.application;

public interface IsbnLookupService {
    String lookupIsbn(String title);
}
```

#### 1-2. BookServiceの変更

`BookService.java`を以下のように変更してください。

**`backend/src/main/java/com/example/library/application/BookService.java`**

```java
package com.example.library.application;

import com.example.library.domain.model.Book;
import com.example.library.domain.repository.BookRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class BookService {

    @Inject
    private BookRepository bookRepository;

    @Inject
    private IsbnLookupService isbnLookupService;

    public Book create(String title, String author, String isbn) {
        if (isbn == null || isbn.isBlank()) {
            try {
                isbn = isbnLookupService.lookupIsbn(title);
            } catch (Exception e) {
                isbn = "UNKNOWN";
            }
        }
        Book book = new Book(title, author, isbn);
        return bookRepository.save(book);
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("書籍が見つかりません: ID=" + id));
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book update(Long id, String title, String author, String isbn) {
        Book book = findById(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        return bookRepository.save(book);
    }

    public void delete(Long id) {
        findById(id);
        bookRepository.deleteById(id);
    }
}
```

#### 1-3. 外部サービス呼び出しの実装（穴埋め）

以下のスケルトンコードをファイルとして作成し、`// TODO` 部分を実装してください。

**`backend/src/main/java/com/example/library/infrastructure/service/ExternalIsbnLookupService.java`**

```java
package com.example.library.infrastructure.service;

import com.example.library.application.IsbnLookupService;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class ExternalIsbnLookupService implements IsbnLookupService {

    private static final String SERVICE_URL = "ここに講師から伝えられたURLを設定";

    @Override
    public String lookupIsbn(String title) {
        // TODO 1: リクエストURLを組み立てる
        //   SERVICE_URL + "/api/isbn-lookup?title=" + URLエンコードしたtitle
        //   ヒント: URLEncoder.encode(title, StandardCharsets.UTF_8)

        // TODO 2: HttpClientでGETリクエストを送信する
        //   HttpClient.newHttpClient() でクライアントを作成
        //   HttpRequest.newBuilder().uri(URI.create(url)).GET().build() でリクエストを作成
        //   client.send(request, HttpResponse.BodyHandlers.ofString()) で送信

        // TODO 3: レスポンスを処理する
        //   ステータスコードが200の場合:
        //     レスポンスボディからISBNを抽出して返す
        //     （ヒント: JSONは {"title":"...","isbn":"..."} 形式。
        //       簡易的にはString操作で抽出できます）
        //   それ以外の場合:
        //     RuntimeExceptionをスローする

        return null; // この行を置き換えてください
    }
}
```

#### 1-4. 既存テストファイルの修正

`BookService`が`IsbnLookupService`に依存するようになったため、既存テストのDI設定にも`ExternalIsbnLookupService`を追加する必要があります。

**`BookServiceTest.java`** — WeldInitiatorに1行追加:

```java
import com.example.library.infrastructure.service.ExternalIsbnLookupService;

// ...

@WeldSetup
WeldInitiator weld = WeldInitiator.of(
    BookService.class,
    JdbcBookRepository.class,
    DataSourceProducer.class,
    TestDatabaseCleaner.class,
    ExternalIsbnLookupService.class  // ← 追加
);
```

**`LoanServiceTest.java`** — 同様にWeldInitiatorに1行追加:

```java
import com.example.library.infrastructure.service.ExternalIsbnLookupService;

// ...

@WeldSetup
WeldInitiator weld = WeldInitiator.of(
    LoanService.class,
    BookService.class,
    MemberService.class,
    JdbcBookRepository.class,
    JdbcMemberRepository.class,
    JdbcLoanRepository.class,
    DataSourceProducer.class,
    TestDatabaseCleaner.class,
    ExternalIsbnLookupService.class  // ← 追加
);
```

#### 1-5. 確認

実装が完了したら、既存のテストが通ることを確認します。

```bash
mvn test
```

> **チェックポイント:**
> - 既存テストがすべてパスしますか？
> - コンパイルエラーはありませんか？
> - エラーが出た場合はエラーメッセージを読んで修正してください

---

### ステップ2: テストの作成（10分）

ISBN自動検索機能のテストを作成します。以下のスケルトンコードをファイルとして作成し、3つのテストメソッドを追加してください。

外部サービスが検索可能な書籍は [isbn-service-booklist.md](isbn-service-booklist.md) を参照してください。

**`backend/src/test/java/com/example/library/application/IsbnLookupBookServiceTest.java`**

```java
package com.example.library.application;

import com.example.library.TestDatabaseCleaner;
import com.example.library.domain.model.Book;
import com.example.library.infrastructure.database.DataSourceProducer;
import com.example.library.infrastructure.repository.JdbcBookRepository;
import com.example.library.infrastructure.service.ExternalIsbnLookupService;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@EnableWeld
class IsbnLookupBookServiceTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.of(
        BookService.class,
        JdbcBookRepository.class,
        DataSourceProducer.class,
        TestDatabaseCleaner.class,
        ExternalIsbnLookupService.class
    );

    @Inject
    BookService bookService;

    @Inject
    TestDatabaseCleaner dbCleaner;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();
    }

    // ここに3つのテストメソッドを追加してください
    //
    // 1. ISBNを省略すると外部サービスから自動取得される
    //    - isbn-service-booklist.md に掲載されている書籍名を使う
    //    - ISBNにnullを渡して、期待されるISBNが返ることを検証
    //
    // 2. ISBNを指定するとそのまま使われる
    //    - ISBNを明示的に渡して、その値がそのまま使われることを検証
    //
    // 3. 存在しない書名の場合ISBNはUNKNOWNになる
    //    - サービスに存在しない書名でISBNをnullにして登録
    //    - ISBNが "UNKNOWN" になることを検証
}
```

なお、テストの実行は次ステップで行います。この時点ではテストコードの記述のみ行ってください。

---

### ステップ3: テストの実行（10分）

作成したテストを実行してみましょう。

```bash
mvn test
```

> **チェックポイント:**
> - テストはすべてパスしましたか？
> - エラーが出た場合はエラーメッセージを読んで修正してください。

次に、テストを複数回実行して結果を観察してください:

```bash
for i in {1..5}; do
  echo "=== Run $i ==="
  mvn test -pl backend -q 2>&1 | tail -3
  echo ""
done
```

> **問いかけ:**
> - 5回すべて同じ結果になりましたか？
> - もしすべてパスしたとしても、このテストは**いつ・どこで実行しても**同じ結果になると言い切れますか？
> - 外部サービスが落ちていたら、どうなりますか？

---

### ステップ4: 原因分析と対策（20分）

#### 4-1. テストの問題点を考える

ステップ2で作成したテストを見直してみましょう。

> **問いかけ:**
> - このテストの成否を決めているのは、テストコード自体ですか？それとも外部の何かですか？
> - 外部サービスが停止・遅延した場合や、ネットワークが不安定な環境（例: CI）で実行した場合、このテストはどうなりますか？
> - このようなテストが持つリスクを説明してください

#### 4-2. 対策を考える

> **問いかけ:**
> - 外部サービスに依存するテストを、どんな環境でも安定させるにはどうすればよいですか？
> - テストでは以下を**確実に再現**できる必要があります:
>   - 外部サービスが**成功する**ケース
>   - 外部サービスが**失敗する**ケース
> - 古典学派のテスト原則では、外部依存をどのように扱いますか？

#### 4-3. 対策の実装

考えた対策方針を実装してみましょう。

> **ヒント（行き詰まった場合）:**
> - テスト用の「偽の」ISBN検索サービスを作る方法はないですか？
> - `IsbnLookupService`インターフェースのテスト専用実装を作れませんか？
> - 「成功を返す実装」と「必ず例外を投げる実装」があれば、
>   両方のケースを決定的にテストできます

#### 4-4. 修正後の検証

修正後、テストを複数回実行して安定性を確認してください。

```bash
for i in {1..5}; do
  echo "=== Run $i ==="
  mvn test -pl backend -q 2>&1 | tail -3
  echo ""
done
```

> **チェックポイント:**
> - 5回すべてテストがパスしましたか？
> - テストは決定的（毎回同じ結果）になりましたか？
> - 「外部サービスが成功するケース」と「失敗するケース」の両方をテストできていますか？

---

### ステップ5: 振り返り

以下の問いについて考えてみましょう。

1. **ステップ2で作成したテストにはどんなリスクがありましたか？**
   - テストで外部サービスをどう扱っていましたか？
   - たとえ今回パスしたとしても、そのテストは信頼できると言えますか？
   - テストの安定性は、誰が担保すべきですか？

2. **Flaky Test（不安定なテスト）がもたらす害は何ですか？**
   - 「たまに失敗するテスト」があるとき、チームはどう対応しがちですか？
   - CIで不安定テストが発生すると、開発フローにどんな影響がありますか？

3. **FIRST原則の「R: Repeatable（繰り返し可能）」とは？**
   - テストはどんな環境・どんなタイミングで実行しても同じ結果を返すべきです
   - 非決定的な要素（ネットワーク、外部サービス、現在時刻、乱数）をテストに
     持ち込むとどうなりますか？

4. **古典学派テストにおける「管理下にない依存（unmanaged dependency）」**
   - 古典学派では、依存を「管理下にある依存」と「管理下にない依存」に分類します
   - 外部サービスやサードパーティAPIなどの「管理下にない依存」は
     テストダブル（偽物）に置き換えます
   - 一方、自分たちのコード（`BookRepository`等）はテストダブルにせず
     本物を使います
   - この区別はなぜ重要ですか？
