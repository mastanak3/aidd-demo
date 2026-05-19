package com.example.library.resource;

import com.example.library.PlaywrightTestBase;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class LoanPlaywrightTest extends PlaywrightTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void setUpData() {
        restTemplate.postForEntity("/api/members",
                Map.of("id", "0000001", "name", "テスト会員", "email", "test@example.com", "memberType", "GENERAL"), Map.class);
        restTemplate.postForEntity("/api/books",
                Map.of("title", "テスト書籍", "author", "テスト著者", "isbn", "ISBN-001"), Map.class);
    }

    @Test
    void 書籍を貸出すると貸出中一覧に表示される() {
        navigateTo("/loans");

        page.locator("select").first().selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("テスト会員"));
        page.locator("select").nth(1).selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("テスト書籍"));
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("貸出")).click();

        assertThat(page.locator("table").first()).containsText("テスト書籍");
        assertThat(page.locator("table").first()).containsText("テスト会員");
    }

    @Test
    void 書籍を返却すると返却済み履歴に移動する() {
        navigateTo("/loans");

        page.locator("select").first().selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("テスト会員"));
        page.locator("select").nth(1).selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("テスト書籍"));
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("貸出")).click();

        assertThat(page.locator("table").first()).containsText("テスト書籍");

        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("返却")).click();

        assertThat(page.getByText("貸出中の書籍はありません")).isVisible();
        assertThat(page.locator("table")).containsText("テスト書籍");
    }

    @Test
    void 貸出後に書籍が貸出可能リストから消える() {
        restTemplate.postForEntity("/api/books",
                Map.of("title", "別の書籍", "author", "別の著者", "isbn", "ISBN-002"), Map.class);

        navigateTo("/loans");

        page.locator("select").first().selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("テスト会員"));
        page.locator("select").nth(1).selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("テスト書籍"));
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("貸出")).click();

        assertThat(page.locator("table").first()).containsText("テスト書籍");

        var bookSelect = page.locator("select").nth(1);
        assertThat(bookSelect).not().containsText("テスト書籍");
        assertThat(bookSelect).containsText("別の書籍");
    }
}
