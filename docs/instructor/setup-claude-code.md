# Claude Code セットアップ手順

このドキュメントでは、DevContainer環境でGCP Vertex AI経由でClaude Codeを利用するためのセットアップ手順を説明します。

## 前提条件

- Visual Studio Code がインストールされていること
- Dev Containers 拡張機能がインストールされていること
- GCPプロジェクトへのアクセス権限があること
- GCPプロジェクトIDを把握していること

## セットアップ手順

### 1. DevContainer環境の起動

1. Visual Studio Codeでこのプロジェクトを開く
2. コマンドパレット（`Ctrl+Shift+P` または `Cmd+Shift+P`）を開く
3. `Dev Containers: Reopen in Container` を選択
4. コンテナのビルドと起動を待つ（初回は数分かかります）

### 2. GCP認証情報の設定

DevContainer内のターミナルで以下を実行します。

#### 2.1 環境変数の設定

```bash
export GCP_PROJECT_ID=your-project-id
export GCP_QUOTA_PROJECT=your-quota-project-id
```

**重要:**
- `your-project-id` を実際のGCPプロジェクトIDに置き換えてください
- `your-quota-project-id` を実際のQuota用プロジェクトIDに置き換えてください（未設定の場合は `GCP_PROJECT_ID` が使用されます）
- これらの情報は認証情報のため、gitにコミットしないでください

#### 2.2 セットアップスクリプトの実行

```bash
source ./setup-claude.local.sh
```

このスクリプトは以下の処理を自動で行います：

1. **GCloud初期化**: プロジェクトIDを設定
2. **Application Default認証**: ブラウザ経由で認証（ブラウザが開きます）
3. **Quota Projectの設定**: `GCP_QUOTA_PROJECT`（未設定時は `GCP_PROJECT_ID`）を設定
4. **環境変数の設定**: `~/.bashrc` に必要な環境変数を追加
   - `GCP_PROJECT_ID`
   - `CLAUDE_CODE_USE_VERTEX=1`
   - `CLOUD_ML_REGION=us-east5`
   - `ANTHROPIC_VERTEX_PROJECT_ID`

#### 2.3 ブラウザ認証

スクリプト実行中にブラウザが開き、Google認証画面が表示されます：

1. GCPプロジェクトへのアクセス権限があるアカウントでログイン
2. 権限の許可を求められたら「許可」をクリック
3. 認証が完了すると、ターミナルに戻ります

### 3. 動作確認

セットアップが完了すると、以下のような出力が表示されます：

```
========================================
  Setup Complete!
========================================

Configuration:
  GCP_PROJECT_ID: your-project-id
  CLAUDE_CODE_USE_VERTEX: 1
  CLOUD_ML_REGION: us-east5
  ANTHROPIC_VERTEX_PROJECT_ID: your-project-id

You're ready to use Claude Code with Vertex AI!
```

環境変数が正しく設定されているか確認：

```bash
echo $GCP_PROJECT_ID
echo $CLAUDE_CODE_USE_VERTEX
echo $CLOUD_ML_REGION
```

### 4. Claude Codeの起動

```bash
claude
```

## トラブルシューティング

### 環境変数が設定されない

新しいターミナルを開いた際に環境変数が反映されない場合：

```bash
source ~/.bashrc
```

または、再度セットアップスクリプトを実行：

```bash
export GCP_PROJECT_ID=your-project-id
export GCP_QUOTA_PROJECT=your-quota-project-id
source ./setup-claude.local.sh
```

### 認証エラーが発生する

認証情報をリセットして再実行：

```bash
gcloud auth application-default revoke
export GCP_PROJECT_ID=your-project-id
export GCP_QUOTA_PROJECT=your-quota-project-id
source ./setup-claude.local.sh
```

### GCPプロジェクトIDを忘れた

現在設定されているプロジェクトIDを確認：

```bash
gcloud config get-value project
```

または、アクセス可能なプロジェクト一覧を表示：

```bash
gcloud projects list
```

## 注意事項

- `GCP_PROJECT_ID` は認証情報のため、**gitにコミットしないでください**
- セットアップスクリプトは必ず `source` コマンドで実行してください
  - `./setup-claude.local.sh` で実行すると環境変数が反映されません
- 環境変数は `~/.bashrc` に保存されるため、次回起動時も有効です
- DevContainerを再ビルドした場合は、再度セットアップが必要です

## 参考情報

- [Claude Code 公式ドキュメント](https://github.com/anthropics/claude-code)
- [GCP Vertex AI ドキュメント](https://cloud.google.com/vertex-ai/docs)
