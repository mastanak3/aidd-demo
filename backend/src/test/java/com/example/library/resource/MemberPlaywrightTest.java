package com.example.library.resource;

import com.example.library.PlaywrightTestBase;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class MemberPlaywrightTest extends PlaywrightTestBase {

    @Test
    void 一般会員を登録して一覧に表示される() {
        navigateTo("/members/new");

        fillFormField("会員ID", "0000001");
        fillFormField("名前", "田中太郎");
        fillFormField("メールアドレス", "tanaka@example.com");
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("登録")).click();

        page.waitForURL("**/members");
        assertThat(page.locator("table")).containsText("田中太郎");
        assertThat(page.locator("table")).containsText("tanaka@example.com");
        assertThat(page.locator("table")).containsText("一般");
    }

    @Test
    void プレミアム会員を登録して一覧に表示される() {
        navigateTo("/members/new");

        fillFormField("会員ID", "0000002");
        fillFormField("名前", "鈴木花子");
        fillFormField("メールアドレス", "suzuki@example.com");
        selectFormField("会員種別", "プレミアム");
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("登録")).click();

        page.waitForURL("**/members");
        assertThat(page.locator("table")).containsText("鈴木花子");
        assertThat(page.locator("table")).containsText("プレミアム");
    }

    @Test
    void 会員を削除すると一覧から消える() {
        navigateTo("/members/new");
        fillFormField("会員ID", "0000003");
        fillFormField("名前", "削除対象会員");
        fillFormField("メールアドレス", "delete@example.com");
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("登録")).click();
        page.waitForURL("**/members");

        assertThat(page.locator("table")).containsText("削除対象会員");

        page.onDialog(dialog -> dialog.accept());
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("削除")).click();

        assertThat(page.getByText("削除対象会員")).not().isVisible();
    }
}
