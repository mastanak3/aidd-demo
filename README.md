# aidd-demo

AI 駆動開発ワークショップ用の**図書貸出管理システム**サンプルアプリケーションです。
書籍・会員の管理と貸出/返却を行う Web アプリケーションで構成されています。

## 技術スタック

| レイヤー | 技術 |
|---------|------|
| フロントエンド | Next.js 16 / React 19 / TypeScript 5 |
| バックエンド | Java 21 / Spring Boot 3.4 / Spring Data JPA |
| データベース | PostgreSQL 16 |
| 外部サービス | ISBN Lookup Service (Python / FastAPI) |
| コード品質 | SonarQube (Community Edition) / JaCoCo |
| ビルド | Maven 3.x / npm |
| 開発環境 | Dev Containers (Docker Compose) |

## ディレクトリ構成

```
backend/       Java バックエンド (REST API)
frontend/      Next.js フロントエンド
isbn-service/  ISBN 検索用の外部サービス (FastAPI)
docs/          ワークショップ資料・開発ガイド
helm/          Helm チャート
.devcontainer/ Dev Container 設定
```

## 開発環境のセットアップ

### Dev Container（推奨）

VS Code で **Dev Containers: Reopen in Container** を実行すると、以下のサービスが自動起動します。

| サービス | ポート | 用途 |
|---------|-------|------|
| app | — | 開発コンテナ (Java 21 / Maven / Node.js) |
| db | 5432 | PostgreSQL 16 |
| sonarqube | 9000 | SonarQube (静的解析) |

### ローカル環境の前提条件

Dev Container を使わない場合は以下が必要です。

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
mvn compile            # ビルド
mvn spring-boot:run    # 起動 (http://localhost:8080/api/)
```

### テストの実行

```bash
cd backend
mvn test       # Playwright以外のテスト（単体テスト + API E2E）
mvn test -Pci  # すべてのテスト（単体 + API E2E + Playwright）
```

| プロファイル | 対象 | 備考 |
|------------|------|------|
| (デフォルト) | 単体 + API E2E | Playwright テストを除外して高速実行 |
| `-Pci` | すべてのテスト | Playwright ブラウザ E2E テストも含めてすべて実行。CI 環境で使用 |

### フロントエンドの起動

```bash
cd frontend
npm install           # 依存パッケージのインストール
npm run dev           # 開発サーバー起動 (http://localhost:3000)
```

API リクエストは `next.config.ts` のリライト設定により `localhost:8080` に自動転送されます。

### 開発時の起動手順

バックエンドとフロントエンドを別ターミナルでそれぞれ起動し、ブラウザで `http://localhost:3000` を開きます。

## SonarQube によるコード品質チェック

### 初期セットアップ（自動）

Dev Container の起動時に `setup-sonarqube.sh` が自動実行され、以下を行います。

1. SonarQube の起動を待機
2. 管理者パスワードを `admin` → 環境変数 `SONAR_ADMIN_PASSWORD`（デフォルト: `sonarpass`）に変更
3. 品質ゲート「CI Gate」を作成しデフォルトに設定

### 品質ゲート「CI Gate」

新規コードに対して以下の条件をチェックします。すべて満たさないとゲート不合格になります。

| メトリクス | 条件 | 閾値 |
|-----------|------|------|
| カバレッジ (`new_coverage`) | 未満で不合格 | **80%** |
| 重複率 (`new_duplicated_lines_density`) | 超過で不合格 | **3%** |
| 信頼性レーティング (`new_reliability_rating`) | A 未満で不合格 | **A** |
| セキュリティレーティング (`new_security_rating`) | A 未満で不合格 | **A** |
| 保守性レーティング (`new_maintainability_rating`) | A 未満で不合格 | **A** |
| セキュリティホットスポット (`new_security_hotspots_reviewed`) | 未レビューで不合格 | **100%** |

上段2つはスクリプトで追加、下段4つは SonarQube が自動付与する CAYC (Clean as You Code) 条件です。

### 解析レポートの送信

```bash
cd backend
mvn clean verify sonar:sonar -Dsonar.login=admin -Dsonar.password=sonarpass
```

このコマンドで以下が一括実行されます。

1. コンパイルとテスト実行
2. JaCoCo によるカバレッジレポート生成
3. SonarQube への解析結果送信
4. 品質ゲートの結果を待機（不合格の場合ビルド失敗）

### 結果の確認

http://localhost:9000 のダッシュボードでプロジェクト **Library Management System** の解析結果（バグ、脆弱性、コードスメル、カバレッジ等）を確認できます。

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
