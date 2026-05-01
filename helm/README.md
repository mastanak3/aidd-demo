# Helm Chart for OpenShift Dev Spaces
## 概要
Red Hat Demo Platformの`Field Sourced Content - OpenShift Base`に渡すHelm Chart。
OpenShiftにDev Spaces OperatorとDev Spacesのインスタンスを構築する。

## 使い方
- [Field Sourced Content - OpenShift Base](https://catalog.demo.redhat.com/catalog/babylon-catalog-prod?item=babylon-catalog-prod/published.ocp-field-asset.prod&utm_source=webapp&utm_medium=share-link)のページにアクセス。
- `Order`ボタンでOpenShiftクラスターの設定ページに移動。
- `Existing Gitops Repo?`にチェックをいれ、以下入力。
  - 本レポジトリのURL: `https://github.com/mastanak3/aidd-demo`
  - ブランチ名: `main`
  - エントリーポイントのディレクトリ名: `helm`
- その他プロジェクト情報を入力し、Orderを確定する。しばらくするとクラスターが払い出される。
- 表示された認証情報を用いてクラスターにログイン。
- `openshift-devspaces`プロジェクト内の`devspaces` Routeリソースを検索。`location`のURLからDashBoardにアクセス可能。
- Workspaceの払出しは、以下で行う。
  - DashBoard操作でのリクエスト。
  - Factory URL (DashBoardのURL + devfileを格納するレポジトリURL)へのアクセスによるリクエスト。