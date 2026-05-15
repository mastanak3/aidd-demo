# 開発ガイド

図書貸出管理システムの開発手順をまとめたドキュメントです。

## 前提条件

- Java 21
- Maven 3.x
- Node.js 18 以上
- npm

## ディレクトリ構成

```
backend/   ... Java バックエンド (JAX-RS / Jersey + Grizzly)
frontend/  ... Next.js フロントエンド
docs/      ... ドキュメント
```

## バックエンド

### ビルド

```bash
cd backend
mvn compile
```

### テスト

```bash
cd backend
mvn test
```

### 起動

```bash
cd backend
mvn exec:java
```

`http://localhost:8080/api/` で API が起動します。Enter キーで停止します。

### 主な API エンドポイント

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

## フロントエンド

### 依存パッケージのインストール

```bash
cd frontend
npm install
```

### 開発サーバーの起動

```bash
cd frontend
npm run dev
```

`http://localhost:3000` でアクセスできます。バックエンド API へのリクエストは `next.config.ts` のリライト設定により `localhost:8080` に転送されます。

### ビルド

```bash
cd frontend
npm run build
```

### プロダクション起動

```bash
cd frontend
npm start
```

## 開発時の起動手順

バックエンドとフロントエンドを別のターミナルでそれぞれ起動してください。

1. バックエンドを起動: `cd backend && mvn exec:java`
2. フロントエンドを起動: `cd frontend && npm run dev`
3. ブラウザで `http://localhost:3000` を開く
