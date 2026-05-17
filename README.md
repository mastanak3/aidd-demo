# 図書館管理システム (Library Management System)

図書館の書籍・会員・貸出を管理するWebアプリケーションです。

## アプリケーション概要

書籍の登録・検索、会員の管理、貸出・返却処理を行う図書館管理システムです。会員種別（一般/プレミアム）に応じた貸出上限・貸出期間・延滞料金のビジネスルールを備えています。

### 主な機能

- **書籍管理** — 書籍の登録・編集・削除・一覧表示
- **会員管理** — 一般/プレミアム会員の登録・編集・削除・一覧表示
- **貸出管理** — 貸出の実行・返却・履歴確認・延滞料金計算

## 技術スタック

| レイヤー | 技術 |
|---------|------|
| **フロントエンド** | Next.js 16, React 19, TypeScript |
| **バックエンド** | WildFly 32 (Bootable JAR), Jakarta EE 10 |
| **REST API** | JAX-RS (RESTEasy) |
| **DI** | CDI (Weld) |
| **永続化** | JPA (Hibernate 6.4) |
| **トランザクション** | JTA |
| **データベース** | PostgreSQL 16 |
| **ビルド** | Maven, npm |
| **テスト** | JUnit 5, Weld JUnit5, REST Assured |
| **Java** | 21 |

## 構成図

```
┌─────────────────┐     ┌──────────────────────────────────────────────┐     ┌──────────────┐
│                 │     │             WildFly Bootable JAR              │     │              │
│    Next.js      │     │                                              │     │  PostgreSQL   │
│   (port 3000)   │────▶│  JAX-RS ──▶ Service ──▶ JPA Repository ─────│────▶│  (port 5432)  │
│                 │     │  Resource    (@Transactional)  (EntityManager)│     │              │
│  /api/* proxy   │     │                (port 8080)                   │     │  DB: library  │
└─────────────────┘     └──────────────────────────────────────────────┘     └──────────────┘
```

フロントエンドの `/api/*` リクエストは Next.js の rewrites でバックエンド (`localhost:8080`) にプロキシされます。

## ディレクトリ構成

```
aidd-demo/
├── backend/                          # バックエンド (Java/Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/library/
│       │   ├── JaxRsApplication.java          # JAX-RS エントリポイント
│       │   ├── application/                   # サービス層
│       │   ├── domain/                        # ドメインモデル・ビジネスルール
│       │   │   ├── model/                     # エンティティ (JPA)
│       │   │   └── repository/                # リポジトリインターフェース
│       │   ├── infrastructure/
│       │   │   ├── database/                  # EntityManager プロデューサー
│       │   │   └── repository/                # JPA リポジトリ実装
│       │   └── resource/                      # REST リソース (JAX-RS)
│       └── test/
├── frontend/                         # フロントエンド (Next.js)
│   ├── package.json
│   └── src/
│       ├── app/                               # ページ (App Router)
│       ├── components/                        # UIコンポーネント
│       └── lib/                               # API クライアント・型定義
├── docs/                             # ワークショップ資料
└── .devcontainer/                    # Dev Container 設定
    └── docker-compose.yml
```

## セットアップ・起動方法

### 前提条件

- Dev Container 環境（推奨）または Java 21 + Node.js + PostgreSQL 16

### PostgreSQL

Dev Container 環境では `db` サービスとして自動起動します。

```bash
# 接続確認
pg_isready -h db

# 手動接続
PGPASSWORD=library psql -h db -U library -d library
```

| 項目 | 値 |
|------|-----|
| ホスト | `db` (Dev Container) / `localhost` (ローカル) |
| ポート | `5432` |
| データベース名 | `library` |
| ユーザー | `library` |
| パスワード | `library` |

### バックエンド

```bash
cd backend

# ビルド（Bootable JAR 生成）
mvn clean package -DskipTests

# 起動
POSTGRESQL_DATABASE=library \
POSTGRESQL_USER=library \
POSTGRESQL_PASSWORD=library \
POSTGRESQL_SERVICE_HOST=db \
POSTGRESQL_SERVICE_PORT=5432 \
java -jar target/library-1.0.0-SNAPSHOT-bootable.jar
```

起動後、`http://localhost:8080/api/books` でAPIにアクセスできます。

#### テスト実行

```bash
# ユニットテスト + サービス層テスト（E2E除く）
mvn test

# 全テスト（E2E含む）
mvn test -Dsurefire.excludedGroups=""

# CI環境（並列実行）
mvn test -Pci
```

### フロントエンド

```bash
cd frontend

# 依存関係インストール
npm install

# 開発サーバー起動
npm run dev
```

起動後、`http://localhost:3000` でアプリにアクセスできます。

## API エンドポイント

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
| POST | `/api/loans` | 貸出実行 |
| POST | `/api/loans/{id}/return` | 返却処理 |
