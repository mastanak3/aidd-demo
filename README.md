# aidd-demo

AI 駆動開発ワークショップ用の**図書貸出管理システム**サンプルアプリケーションです。
書籍・会員の管理と貸出/返却を行う Web アプリケーションで構成されています。

## 技術スタック

| レイヤー | 技術 |
|---------|------|
| フロントエンド | Next.js 16 / React 19 / TypeScript |
| バックエンド | Java 21 / JAX-RS (Jersey) + Grizzly |
| データベース | PostgreSQL 16 (HikariCP 接続プール) |
| 外部サービス | ISBN Lookup Service (Python / FastAPI) |
| ビルド | Maven 3.x / npm |

## ディレクトリ構成

```
backend/       Java バックエンド (REST API)
frontend/      Next.js フロントエンド
isbn-service/  ISBN 検索用の外部サービス (FastAPI)
docs/          ワークショップ資料・開発ガイド
helm/          Helm チャート
```

## 開発環境のセットアップ

### 前提条件

- Java 21 / Maven 3.x
- Node.js 18 以上 / npm
- PostgreSQL 16

### データベース

PostgreSQL に以下の設定で接続します（環境変数で上書き可）。

| 項目 | デフォルト値 | 環境変数 |
|------|------------|---------|
| JDBC URL | `jdbc:postgresql://localhost:5432/library` | `DATABASE_URL` |
| ユーザー | `library` | `DATABASE_USER` |
| パスワード | `library` | `DATABASE_PASSWORD` |

`psql` でのログイン例:

```bash
psql -h localhost -U library -d library
```

スキーマ（`books`, `members`, `loans` テーブル）はバックエンド起動時に自動作成されます。

### バックエンドの起動

```bash
cd backend
mvn compile          # ビルド
mvn test             # テスト実行
mvn exec:java        # 起動 (http://localhost:8080/api/)
```

### フロントエンドの起動

```bash
cd frontend
npm install           # 依存パッケージのインストール
npm run dev           # 開発サーバー起動 (http://localhost:3000)
```

API リクエストは `next.config.ts` のリライト設定により `localhost:8080` に自動転送されます。

### 開発時の起動手順

バックエンドとフロントエンドを別ターミナルでそれぞれ起動し、ブラウザで `http://localhost:3000` を開きます。

## API エンドポイント

| メソッド | パス | 説明 |
|---------|------|------|
| GET | /api/books | 書籍一覧 |
| POST | /api/books | 書籍登録 |
| PUT | /api/books/{id} | 書籍更新 |
| DELETE | /api/books/{id} | 書籍削除 |
| GET | /api/members | 会員一覧 |
| POST | /api/members | 会員登録 |
| PUT | /api/members/{id} | 会員更新 |
| DELETE | /api/members/{id} | 会員削除 |
| GET | /api/loans | 貸出一覧 |
| POST | /api/loans | 貸出実行 |
| POST | /api/loans/{id}/return | 返却 |
