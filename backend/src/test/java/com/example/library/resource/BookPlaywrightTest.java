package com.example.library.resource;

import com.example.library.PlaywrightTestBase;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class BookPlaywrightTest extends PlaywrightTestBase {

    @Test
    void 書籍を登録して一覧に表示される() {
        navigateTo("/books/new");

        fillFormField("タイトル", "テスト駆動開発");
        fillFormField("著者", "Kent Beck");
        fillFormField("ISBN", "978-4-274-21788-0");
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("登録")).click();

        page.waitForURL("**/books");
        assertThat(page.locator("table")).containsText("テスト駆動開発");
        assertThat(page.locator("table")).containsText("Kent Beck");
    }

    @Test
    void 書籍を削除すると一覧から消える() {
        navigateTo("/books/new");
        fillFormField("タイトル", "削除対象書籍");
        fillFormField("著者", "著者X");
        fillFormField("ISBN", "ISBN-DEL");
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("登録")).click();
        page.waitForURL("**/books");

        assertThat(page.locator("table")).containsText("削除対象書籍");

        page.onDialog(dialog -> dialog.accept());
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("削除")).click();

        assertThat(page.getByText("削除対象書籍")).not().isVisible();
    }
}
