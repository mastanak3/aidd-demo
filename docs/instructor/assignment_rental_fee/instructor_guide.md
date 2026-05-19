# インストラクターガイド: 貸出料金計算機能

本ドキュメントは、課題の各ラウンドで期待されるコードと指導上のポイントをまとめたものです。

---

## ファイル一覧

### 新規作成（参加者が作成）

| ファイル | パッケージ / パス | ラウンド |
|---------|-----------------|---------|
| `RentalFeeCalculatorTest.java` | `src/test` ─ `domain/` | 1〜3 |

### 変更（参加者が変更）

| ファイル | 変更内容 | ラウンド |
|---------|---------|---------|
| `RentalFeeCalculator.java` | `calculateFee()` のロジック実装 | 1〜3 |

### 事前準備（講師が作成・変更）

| ファイル | 内容 |
|---------|------|
| `RentalFeeCalculator.java` | スケルトン（`return 0;`）を `domain/` に配置 |
| `Loan.java` | `rentalFee` フィールドを追加 |
| `LoanService.java` | `borrowBook()` 内で `RentalFeeCalculator.calculateFee()` を呼び出し、`Loan` に料金をセット |
| フロントエンド | 貸出料金の表示を追加 |

---

## 事前準備の詳細

### `RentalFeeCalculator.java`（スケルトン）

`src/main/java/com/example/library/domain/` に配置:

```java
package com.example.library.domain;

import com.example.library.domain.model.MemberType;

public class RentalFeeCalculator {

    public static int calculateFee(MemberType memberType, int rentalDays, boolean isNewRelease) {
        return 0; // TODO: 実装してください
    }
}
```

### `Loan.java` への変更

`rentalFee` フィールドを追加:

```java
@Column(name = "rental_fee")
private int rentalFee;

public int getRentalFee() {
    return rentalFee;
}

public void setRentalFee(int rentalFee) {
    this.rentalFee = rentalFee;
}
```

### `schema.sql` への変更

`loans` テーブルに `rental_fee` カラムを追加:

```sql
rental_fee INTEGER DEFAULT 0
```

### `LoanService.java` への変更

`borrowBook()` 内で料金を計算してセット:

```java
Loan loan = new Loan(bookId, memberId, loanDate, dueDate);
int rentalFee = RentalFeeCalculator.calculateFee(
        member.getMemberType(),
        LendingPolicy.getLoanPeriodDays(member.getMemberType()),
        book.isNewRelease());  // ※ Book に isNewRelease を追加するか、引数で受け取る
loan.setRentalFee(rentalFee);
return loanRepository.save(loan);
```

> **注:** `isNewRelease` の取得方法はアプリ設計に依存します。`Book` エンティティにフィールドを追加するか、API リクエストのパラメータとして受け取る方法があります。

---

## ラウンド1: 基本料金の計算

### 期待されるコード

#### `RentalFeeCalculatorTest.java`（ラウンド1時点）

```java
package com.example.library.domain;

import com.example.library.domain.model.MemberType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RentalFeeCalculatorTest {

    @Test
    void 一般会員が通常書籍を3日借りると300円() {
        assertEquals(300,
            RentalFeeCalculator.calculateFee(MemberType.GENERAL, 3, false));
    }

    @Test
    void プレミアム会員が通常書籍を3日借りると240円() {
        assertEquals(240,
            RentalFeeCalculator.calculateFee(MemberType.PREMIUM, 3, false));
    }
}
```

#### `RentalFeeCalculator.java`（ラウンド1時点）

```java
package com.example.library.domain;

import com.example.library.domain.model.MemberType;

public class RentalFeeCalculator {

    public static int calculateFee(MemberType memberType, int rentalDays, boolean isNewRelease) {
        int dailyRate = switch (memberType) {
            case GENERAL -> 100;
            case PREMIUM -> 80;
        };
        return dailyRate * rentalDays;
    }
}
```

### テスト実行結果

```
mvn test -pl backend -Dtest="*RentalFeeCalculatorTest"
```

```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 指導ポイント

- **Red フェーズで `return 0;` のまま実行することの意味:**
  「テストが失敗することを確認する」のが Red フェーズの目的。テストが最初から通ってしまうなら、そのテストは何も検証していない可能性がある。

- **`switch` 式を知らない参加者がいる場合:**
  Java 14 から導入された `switch` 式を使うと、値を直接返せる。従来の `switch` 文 + `break` や `if-else` で書いても問題ない。

- **定数を使うかどうか:**
  Green フェーズではマジックナンバーのままでよい。Refactor フェーズで定数に切り出すのが TDD の流れ。ただし、この時点で定数化しても問題ない。

---

## ラウンド2: 新刊加算

### 期待されるコード

#### `RentalFeeCalculatorTest.java`（ラウンド2で追加するテスト）

```java
@Test
void 一般会員が新刊を3日借りると450円() {
    assertEquals(450,
        RentalFeeCalculator.calculateFee(MemberType.GENERAL, 3, true));
}

@Test
void プレミアム会員が新刊を3日借りても加算なしで240円() {
    assertEquals(240,
        RentalFeeCalculator.calculateFee(MemberType.PREMIUM, 3, true));
}
```

#### `RentalFeeCalculator.java`（ラウンド2時点）

```java
package com.example.library.domain;

import com.example.library.domain.model.MemberType;

public class RentalFeeCalculator {

    public static int calculateFee(MemberType memberType, int rentalDays, boolean isNewRelease) {
        int dailyRate = switch (memberType) {
            case GENERAL -> 100;
            case PREMIUM -> 80;
        };

        int fee = dailyRate * rentalDays;

        if (isNewRelease && memberType != MemberType.PREMIUM) {
            fee = (int)(fee * 1.5);
        }

        return fee;
    }
}
```

### テスト実行結果

```
mvn test -pl backend -Dtest="*RentalFeeCalculatorTest"
```

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 指導ポイント

- **テスト4が通るがテスト3が失敗するパターン:**
  `isNewRelease` の条件だけで分岐し、プレミアム会員の除外を忘れると、テスト3（プレミアム会員の新刊免除）が失敗する。TDD のテストが仕様の漏れを検知した好例として説明する。

- **条件の書き方のバリエーション:**
  以下はすべて同じ意味。参加者の書き方を否定せず、読みやすさを議論する材料にする:

  ```java
  // パターン1: != で除外
  if (isNewRelease && memberType != MemberType.PREMIUM)

  // パターン2: == で対象を明示
  if (isNewRelease && memberType == MemberType.GENERAL)
  ```

  パターン2は、今後会員種別が追加された場合に新しい種別が漏れる可能性がある。パターン1の方が安全だが、どちらが正しいかは要件次第。

- **`(int)(fee * 1.5)` の端数切り捨てについて:**
  現在のテストケースでは端数は発生しない（100×3=300, 300×1.5=450）。実務では端数が発生するケースもテストすべきだが、この課題では省略している。

---

## ラウンド3: 上限額の適用

### 期待されるコード

#### `RentalFeeCalculatorTest.java`（ラウンド3で追加するテスト）

```java
@Test
void 一般会員が新刊を10日借りると上限の1000円() {
    assertEquals(1000,
        RentalFeeCalculator.calculateFee(MemberType.GENERAL, 10, true));
}

@Test
void プレミアム会員が通常書籍を11日借りると上限の800円() {
    assertEquals(800,
        RentalFeeCalculator.calculateFee(MemberType.PREMIUM, 11, false));
}
```

#### `RentalFeeCalculator.java`（最終形）

```java
package com.example.library.domain;

import com.example.library.domain.model.MemberType;

public class RentalFeeCalculator {

    public static int calculateFee(MemberType memberType, int rentalDays, boolean isNewRelease) {
        int dailyRate = switch (memberType) {
            case GENERAL -> 100;
            case PREMIUM -> 80;
        };

        int fee = dailyRate * rentalDays;

        if (isNewRelease && memberType != MemberType.PREMIUM) {
            fee = (int)(fee * 1.5);
        }

        int maxFee = switch (memberType) {
            case GENERAL -> 1000;
            case PREMIUM -> 800;
        };

        return Math.min(fee, maxFee);
    }
}
```

### テスト実行結果

```
mvn test -pl backend -Dtest="*RentalFeeCalculatorTest"
```

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 指導ポイント

- **上限のテストで `Math.min` を使わずに `if` で書く参加者がいる場合:**

  ```java
  if (fee > maxFee) {
      fee = maxFee;
  }
  ```

  これでも正しい。`Math.min` はよりコンパクトだが、可読性の観点ではどちらでも問題ない。

- **Refactor フェーズで定数化を促す:**
  最終形では 100, 80, 1000, 800 などのマジックナンバーが残っている。既存の `LendingPolicy.java` を参考に、定数として切り出すリファクタリングを促すとよい:

  ```java
  private static final int GENERAL_DAILY_RATE = 100;
  private static final int PREMIUM_DAILY_RATE = 80;
  private static final int GENERAL_MAX_FEE = 1000;
  private static final int PREMIUM_MAX_FEE = 800;
  ```

- **2つの `switch` 式をまとめたい参加者がいる場合:**
  日額と上限額を1つの `switch` でまとめようとする参加者がいるかもしれない。例:

  ```java
  int dailyRate, maxFee;
  switch (memberType) {
      case GENERAL -> { dailyRate = 100; maxFee = 1000; }
      case PREMIUM -> { dailyRate = 80;  maxFee = 800; }
  }
  ```

  これも有効なアプローチ。どちらが読みやすいかを議論する材料にする。

---

## 完成コード一覧

### `RentalFeeCalculatorTest.java`（最終形）

```java
package com.example.library.domain;

import com.example.library.domain.model.MemberType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RentalFeeCalculatorTest {

    @Test
    void 一般会員が通常書籍を3日借りると300円() {
        assertEquals(300,
            RentalFeeCalculator.calculateFee(MemberType.GENERAL, 3, false));
    }

    @Test
    void プレミアム会員が通常書籍を3日借りると240円() {
        assertEquals(240,
            RentalFeeCalculator.calculateFee(MemberType.PREMIUM, 3, false));
    }

    @Test
    void 一般会員が新刊を3日借りると450円() {
        assertEquals(450,
            RentalFeeCalculator.calculateFee(MemberType.GENERAL, 3, true));
    }

    @Test
    void プレミアム会員が新刊を3日借りても加算なしで240円() {
        assertEquals(240,
            RentalFeeCalculator.calculateFee(MemberType.PREMIUM, 3, true));
    }

    @Test
    void 一般会員が新刊を10日借りると上限の1000円() {
        assertEquals(1000,
            RentalFeeCalculator.calculateFee(MemberType.GENERAL, 10, true));
    }

    @Test
    void プレミアム会員が通常書籍を11日借りると上限の800円() {
        assertEquals(800,
            RentalFeeCalculator.calculateFee(MemberType.PREMIUM, 11, false));
    }
}
```

### `RentalFeeCalculator.java`（最終形・定数化後）

```java
package com.example.library.domain;

import com.example.library.domain.model.MemberType;

public class RentalFeeCalculator {

    private static final int GENERAL_DAILY_RATE = 100;
    private static final int PREMIUM_DAILY_RATE = 80;
    private static final int GENERAL_MAX_FEE = 1000;
    private static final int PREMIUM_MAX_FEE = 800;

    public static int calculateFee(MemberType memberType, int rentalDays, boolean isNewRelease) {
        int dailyRate = switch (memberType) {
            case GENERAL -> GENERAL_DAILY_RATE;
            case PREMIUM -> PREMIUM_DAILY_RATE;
        };

        int fee = dailyRate * rentalDays;

        if (isNewRelease && memberType != MemberType.PREMIUM) {
            fee = (int)(fee * 1.5);
        }

        int maxFee = switch (memberType) {
            case GENERAL -> GENERAL_MAX_FEE;
            case PREMIUM -> PREMIUM_MAX_FEE;
        };

        return Math.min(fee, maxFee);
    }
}
```

---

## トラブルシューティング

### よくある質問と対応

| 症状 | 原因 | 対応 |
|------|------|------|
| テストが最初から通ってしまう | `assertEquals(0, ...)` と書いてしまった | 期待値が仕様通りの値（300, 240 等）になっているか確認する |
| プレミアム会員の新刊テストが失敗する | `isNewRelease` だけで分岐し、会員種別の条件を忘れている | `memberType != MemberType.PREMIUM` の条件を追加する |
| 上限テストで期待値と1だけずれる | `(int)(fee * 1.5)` の端数処理の問題 | 現在のテストケースでは端数は発生しないため、計算過程を見直す |
| 既存テスト（`LoanServiceTest` 等）が壊れる | `RentalFeeCalculator` のスケルトンが配置されていない、または `LoanService` の変更が不完全 | 事前準備が正しく行われているか確認する |
| `switch` 式でコンパイルエラー | Java バージョンが 14 未満、または `switch` 文の構文で書いている | `->` 構文（switch 式）か `:` 構文（switch 文）かを確認。プロジェクトは Java 21 なので switch 式が使える |
