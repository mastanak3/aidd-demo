# 図書貸出管理システム — ベースラインプロジェクト

## 概要

シンプルなCRUDでありながら、会員種別ごとの貸出上限・貸出期間など細かいドメインルールが自然に生まれる題材です。

**機能:**
- 書籍CRUD（タイトル、著者、ISBN、貸出状態）
- 会員CRUD（氏名、メールアドレス、会員種別: 一般/プレミアム）
- 貸出・返却（書籍を借りる・返す）

**ドメインルール:**

| 会員種別 | 同時貸出上限 | 貸出期間 |
|----------|-------------|---------|
| 一般     | 3冊         | 14日    |
| プレミアム | 10冊       | 30日    |

---

## 技術スタック

| カテゴリ | 技術 | バージョン |
|---------|------|-----------|
| 言語 | Java | 17 |
| ビルド | Maven | 3.9+ |
| REST API | Jersey (JAX-RS 3.1) + Grizzly | 3.1.5 |
| DI (CDI) | Weld SE | 5.1.2.Final |
| JSON | Jackson + JavaTimeModule | 2.16.x |
| テスト | JUnit 5 | 5.10.2 |
| 結合テスト | weld-junit5 | 4.0.2.Final |
| E2Eテスト | REST Assured | 5.4.0 |

---

## プロジェクト構成

```
src/main/java/com/example/library/
├── App.java                          # アプリケーション起動 (Weld SE + Jersey + Grizzly)
├── domain/
│   ├── model/
│   │   ├── Book.java                 # 書籍 (checkout/returnBook)
│   │   ├── Member.java               # 会員
│   │   ├── MemberType.java           # 会員種別 enum (GENERAL / PREMIUM)
│   │   └── Loan.java                 # 貸出 (isActive/returnBook)
│   ├── LendingPolicy.java            # 貸出ポリシー (上限・期間の計算)
│   └── repository/                   # Repository インターフェース
│       ├── BookRepository.java
│       ├── MemberRepository.java
│       └── LoanRepository.java
├── application/                      # アプリケーションサービス (@ApplicationScoped)
│   ├── BookService.java
│   ├── MemberService.java
│   └── LoanService.java
├── infrastructure/repository/        # InMemory リポジトリ実装
│   ├── InMemoryBookRepository.java
│   ├── InMemoryMemberRepository.java
│   └── InMemoryLoanRepository.java
└── resource/                         # JAX-RS リソース
    ├── BookResource.java             # /api/books
    ├── MemberResource.java           # /api/members
    ├── LoanResource.java             # /api/loans
    ├── JacksonConfig.java            # ObjectMapper 設定 (JavaTimeModule)
    └── ExceptionMappers.java         # 例外→HTTPステータス変換
```

**設計ポイント:**
- ドメインロジックはPOJO（`LendingPolicy`、`Book`、`Loan`等）に隔離
- Resource → ApplicationService → Domain の層構造
- Repository インターフェースで永続化を抽象化（実装は InMemory）

---

## テスト構成

| 種類 | テストクラス | 件数 | 概要 |
|------|-------------|------|------|
| **ドメイン単体** | `LendingPolicyTest` | 11 | 貸出上限・期間・借用可否の検証 |
| | `BookTest` | 5 | 貸出状態の変更・二重貸出エラー |
| | `MemberTest` | 2 | 会員の生成 |
| **結合 (Weld-Testing)** | `BookServiceTest` | 6 | 書籍CRUD操作 |
| | `MemberServiceTest` | 7 | 会員CRUD操作 |
| | `LoanServiceTest` | 11 | 貸出・返却・上限超えエラー |
| **E2E (REST Assured)** | `BookResourceTest` | 5 | 書籍REST API |
| | `MemberResourceTest` | 6 | 会員REST API |
| | `LoanResourceTest` | 4 | 貸出・返却フロー |
| | **合計** | | **57件** |

- テストスタイル: 古典学派（モック不使用）
- テストピラミッド: ドメイン単体テスト → Weld-Testing結合テスト → REST Assured E2Eテスト

---

## 動作確認手順

### 1. テストの実行

```bash
mvn clean test
```

全57件のテストがパスすることを確認してください。

### 2. アプリケーションの起動

```bash
mvn exec:java
```

`Library API started at http://localhost:8080/api/` と表示されれば起動成功です。

### 3. API の動作確認

別のターミナルから以下のコマンドを実行してください。

**書籍を登録する:**

```bash
curl -s -X POST http://localhost:8080/api/books \
  -H 'Content-Type: application/json' \
  -d '{"title": "テスト駆動開発", "author": "Kent Beck", "isbn": "978-4-274-21788-0"}' | jq .
```

**会員を登録する:**

```bash
curl -s -X POST http://localhost:8080/api/members \
  -H 'Content-Type: application/json' \
  -d '{"name": "田中太郎", "email": "tanaka@example.com", "memberType": "GENERAL"}' | jq .
```

**書籍を貸出する:**

```bash
curl -s -X POST http://localhost:8080/api/loans \
  -H 'Content-Type: application/json' \
  -d '{"memberId": 1, "bookId": 1}' | jq .
```

レスポンスに `loanDate`、`dueDate`（貸出日から14日後）が含まれることを確認してください。

**書籍を返却する:**

```bash
curl -s -X POST http://localhost:8080/api/loans/1/return \
  -H 'Content-Type: application/json' | jq .
```

レスポンスの `returnDate` に返却日が、`active` が `false` になることを確認してください。

**全書籍を取得する:**

```bash
curl -s http://localhost:8080/api/books | jq .
```

### 4. アプリケーションの停止

起動中のターミナルで Enter キーを押すと停止します。

---

## REST API 一覧

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/books` | 全書籍取得 |
| GET | `/api/books/{id}` | 書籍取得 |
| POST | `/api/books` | 書籍登録 |
| PUT | `/api/books/{id}` | 書籍更新 |
| DELETE | `/api/books/{id}` | 書籍削除 |
| GET | `/api/members` | 全会員取得 |
| GET | `/api/members/{id}` | 会員取得 |
| POST | `/api/members` | 会員登録 |
| PUT | `/api/members/{id}` | 会員更新 |
| DELETE | `/api/members/{id}` | 会員削除 |
| GET | `/api/loans` | 全貸出取得 |
| GET | `/api/loans/{id}` | 貸出取得 |
| POST | `/api/loans` | 貸出（書籍を借りる） |
| POST | `/api/loans/{id}/return` | 返却（書籍を返す） |
