# ミューテーションテストパターン集

このドキュメントでは、テストの品質を検証するためのミューテーション（変異）パターンをまとめています。
これらのパターンを使って、既存のテストがバグを検知できるか確認できます。

## 実際に試したミューテーションパターン

### 1. 定数値の変更

**パターン**: ビジネスルールで使用される定数を変更する

**観点**:
- 具体的な期待値を検証しているか
- 計算結果が定数に依存する場合、その定数の正しさを検証しているか

**例**:
```java
// 元のコード
private static final int GENERAL_LATE_FEE_PER_DAY = 50;

// ミューテーション
private static final int GENERAL_LATE_FEE_PER_DAY = 30;
```

**検知するテスト**:
```java
// ✓ 具体的な期待値を検証
assertEquals(250, fee.amount());  // 5日 × 50円

// ✗ 存在だけ確認（定数が変わっても気づけない）
assertNotNull(fee);
assertTrue(fee.amount() > 0);
```

---

### 2. 境界値条件の変更

**パターン**: 比較演算子を変更する（`<` ↔ `<=`, `>` ↔ `>=`）

**観点**:
- 境界値の両側（境界の内側と外側）をテストしているか
- 境界値そのものをテストしているか

**例**:
```java
// 元のコード
if (overdueDays <= 0) {
    return new LateFee(0);
}

// ミューテーション
if (overdueDays < 0) {
    return new LateFee(0);
}
```

**検知するテスト**:
```java
// ✓ 境界値の両側をテスト
assertEquals(0, calculateFee(dueDate, dueDate));           // 境界（当日）
assertEquals(0, calculateFee(dueDate, dueDate.minusDays(1))); // 内側（前日）
assertEquals(50, calculateFee(dueDate, dueDate.plusDays(1))); // 外側（翌日）

// ✗ 境界値の片側だけ、または偶然同じ結果になるケース
assertEquals(0, calculateFee(dueDate, dueDate));  // 0 * rate = 0 で偶然通る
```

---

### 3. 条件分岐の論理反転

**パターン**: switch/if文の条件を入れ替える、逆にする

**観点**:
- すべての分岐パターンをテストしているか
- 各分岐で期待される結果が明確か

**例**:
```java
// 元のコード
int feePerDay = switch (memberType) {
    case GENERAL -> GENERAL_LATE_FEE_PER_DAY;
    case PREMIUM -> PREMIUM_LATE_FEE_PER_DAY;
};

// ミューテーション
int feePerDay = switch (memberType) {
    case GENERAL -> PREMIUM_LATE_FEE_PER_DAY;  // 逆
    case PREMIUM -> GENERAL_LATE_FEE_PER_DAY;  // 逆
};
```

**検知するテスト**:
```java
// ✓ 各パターンの具体的な期待値をテスト
assertEquals(250, calculateFee(GENERAL, 5));   // 5 × 50円
assertEquals(100, calculateFee(PREMIUM, 5));   // 5 × 20円

// ✗ パターンごとに異なる値を検証していない
assertTrue(calculateFee(GENERAL, 5) > 0);
assertTrue(calculateFee(PREMIUM, 5) > 0);
```

---

### 4. 算術演算子の変更

**パターン**: 演算子を変更する（`+` ↔ `-`, `*` ↔ `/`, `+1` ↔ `-1`）

**観点**:
- 計算結果の具体的な値を検証しているか
- オフバイワンエラーを検知できるか

**例**:
```java
// 元のコード
int calculatedFee = (int) (overdueDays * feePerDay);

// ミューテーション
int calculatedFee = (int) ((overdueDays - 1) * feePerDay);
```

**検知するテスト**:
```java
// ✓ 具体的な計算結果を検証
assertEquals(250, calculateFee(5));  // 5日 × 50円

// ✗ 計算が正しいか検証していない
assertTrue(calculateFee(5) > 0);
assertNotNull(calculateFee(5));
```

---

### 5. 関数呼び出しの変更

**パターン**: `Math.min` ↔ `Math.max`, メソッド呼び出しの入れ替え

**観点**:
- 関数の意図（最小値/最大値の選択など）を検証しているか
- 上限・下限の両方のケースをテストしているか

**例**:
```java
// 元のコード
int finalFee = Math.min(calculatedFee, maxFee);

// ミューテーション
int finalFee = Math.max(calculatedFee, maxFee);
```

**検知するテスト**:
```java
// ✓ 上限に達するケースと達しないケースの両方をテスト
assertEquals(1000, calculateFee(40));  // 2000円 → 上限1000円
assertEquals(950, calculateFee(19));   // 950円（上限未達）

// ✗ 上限に達するケースのみ
assertEquals(1000, calculateFee(40));
```

---

## テストコードの潜在的な弱点パターン

### 6. 境界値の0を明示的にテストしていない

**パターン**: 負の値のテストはあるが、0のテストがない

**観点**:
- `< 0` と `<= 0` の違いを検知できるか
- 0が有効な値か無効な値かを明確にテストしているか

**例**:
```java
// テストにある
assertThrows(IllegalArgumentException.class, () -> new LateFee(-1));

// テストにない（弱点）
LateFee fee = new LateFee(0);  // 0は有効か？
```

**改善**:
```java
@Test
void 金額0円のLateFeeを生成できる() {
    LateFee lateFee = new LateFee(0);
    assertEquals(0, lateFee.amount());
}
```

---

### 7. 例外メッセージの検証がない

**パターン**: 例外の型だけチェックし、メッセージは検証していない

**観点**:
- エラーメッセージがユーザーに適切な情報を提供しているか
- メッセージが意図せず変更されていないか

**例**:
```java
// 弱いテスト
assertThrows(IllegalStateException.class, () -> operation());

// 強いテスト
IllegalStateException ex = assertThrows(IllegalStateException.class, 
    () -> operation());
assertTrue(ex.getMessage().contains("返却済み"));
```

---

### 8. 境界値の片側だけテスト

**パターン**: 境界値より大きい/小さい値のどちらか一方だけテスト

**観点**:
- 境界の内側と外側の両方をテストしているか
- 境界値そのものをテストしているか

**例**:
```java
// 弱いテスト（片側のみ）
assertTrue(canBorrow(2));   // 上限3の内側
assertFalse(canBorrow(3));  // 境界

// 改善（両側をテスト）
assertTrue(canBorrow(maxLoans - 1));   // 境界の内側
assertFalse(canBorrow(maxLoans));      // 境界
assertFalse(canBorrow(maxLoans + 1));  // 境界の外側
```

---

### 9. 上限・下限の片方だけテスト

**パターン**: 最大値/最小値のどちらか一方だけテスト

**観点**:
- 上限に達するケースと達しないケースの両方をテストしているか
- 下限に達するケースと達しないケースの両方をテストしているか

**例**:
```java
// 弱いテスト（上限到達のみ）
assertEquals(1000, calculateFee(40));  // 2000 → 上限1000

// 改善（上限到達と未達の両方）
assertEquals(1000, calculateFee(40));  // 上限到達
assertEquals(950, calculateFee(19));   // 上限未達
```

---

### 10. 具体的な値ではなく存在だけ確認

**パターン**: `assertNotNull`, `assertTrue(x > 0)` などの弱いアサーション

**観点**:
- 計算結果が正しい値かを検証しているか
- ビジネスルールに基づく期待値を明確にテストしているか

**例**:
```java
// 弱いテスト
assertNotNull(fee);
assertTrue(fee.amount() >= 0);

// 強いテスト
assertEquals(250, fee.amount());  // 5日 × 50円 = 250円
```

---

## ミューテーションテストを実施する際のチェックリスト

### テスト設計時
- [ ] すべての境界値（0, 上限, 下限）を明示的にテストしているか
- [ ] 境界値の両側（内側と外側）をテストしているか
- [ ] 具体的な期待値を検証しているか（存在確認だけでないか）
- [ ] すべての分岐パターンをテストしているか
- [ ] 各パターンで異なる結果を検証しているか

### テストレビュー時
- [ ] 定数を変更しても検知できるか
- [ ] `<` を `<=` に変更しても検知できるか
- [ ] 条件分岐を入れ替えても検知できるか
- [ ] `+1` を `-1` に変更しても検知できるか
- [ ] `Math.min` を `Math.max` に変更しても検知できるか

### アサーション選択時
- [ ] `assertNotNull` → 可能なら `assertEquals` で具体的な値を検証
- [ ] `assertTrue(x > 0)` → 可能なら `assertEquals(expected, x)`
- [ ] `assertThrows` → メッセージも検証する
- [ ] ブール値の検証 → 期待値を明示（`assertTrue(x)` より `assertEquals(true, x)`）

---

## 参考: ミューテーションテストツール

実際にミューテーションテストを自動化する場合、以下のツールが利用できます：

- **PITest (Java)**: https://pitest.org/
- **Stryker (JavaScript/TypeScript)**: https://stryker-mutator.io/
- **mutmut (Python)**: https://github.com/boxed/mutmut

これらのツールは、コードを自動的に変異させてテストスイートを実行し、
テストの品質を測定します。

---

## まとめ

カバレッジ100%は良いスタート地点ですが、**テストが本当にバグを検知できるか**が重要です。

- **定数値**: 具体的な期待値を検証する
- **境界値**: 両側と境界そのものをテストする
- **条件分岐**: すべてのパターンで異なる結果を検証する
- **計算**: 具体的な計算結果を検証する
- **上限下限**: 到達・未達の両方をテストする

「存在確認」ではなく「値の検証」を行うことで、より堅牢なテストスイートを構築できます。
