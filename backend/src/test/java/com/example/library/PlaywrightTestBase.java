package com.example.library;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;

@Tag("playwright")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public abstract class PlaywrightTestBase {

    private static final String FRONTEND_URL = "http://localhost:3000";
    private static final int FRONTEND_STARTUP_TIMEOUT_MS = 120_000;

    private static Playwright playwright;
    private static Browser browser;
    private static Process frontendProcess;
    private static boolean managedFrontend;

    protected Page page;

    @Autowired
    TestDatabaseCleaner cleaner;

    @BeforeAll
    static void startPlaywright() throws Exception {
        startFrontendIfNeeded();
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void stopPlaywright() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        if (managedFrontend) stopFrontend();
    }

    @BeforeEach
    void setUpPage() {
        cleaner.cleanAll();
        page = browser.newPage();
    }

    @AfterEach
    void tearDownPage() {
        if (page != null) page.close();
    }

    protected void navigateTo(String path) {
        page.navigate(FRONTEND_URL + path);
    }

    protected void fillFormField(String labelText, String value) {
        page.locator(".form-group").filter(
                new Locator.FilterOptions().setHasText(labelText)
        ).locator("input").fill(value);
    }

    protected void selectFormField(String labelText, String optionLabel) {
        page.locator(".form-group").filter(
                new Locator.FilterOptions().setHasText(labelText)
        ).locator("select").selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(optionLabel));
    }

    private static void startFrontendIfNeeded() throws Exception {
        if (isFrontendRunning()) {
            managedFrontend = false;
            return;
        }
        managedFrontend = true;

        Path frontendDir = Path.of("../frontend").toAbsolutePath().normalize();
        if (!frontendDir.resolve("node_modules").toFile().exists()) {
            Process install = new ProcessBuilder("npm", "install")
                    .directory(frontendDir.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (install.waitFor() != 0) {
                throw new RuntimeException("npm install failed");
            }
        }

        ProcessBuilder pb = new ProcessBuilder("npm", "run", "dev")
                .directory(frontendDir.toFile());
        pb.redirectOutput(new File("/tmp/playwright-frontend.log"));
        pb.redirectErrorStream(true);
        frontendProcess = pb.start();

        waitForFrontend();
    }

    private static boolean isFrontendRunning() {
        try {
            HttpURLConnection conn = (HttpURLConnection)
                    URI.create(FRONTEND_URL).toURL().openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code < 500;
        } catch (IOException e) {
            return false;
        }
    }

    private static void waitForFrontend() throws Exception {
        long deadline = System.currentTimeMillis() + FRONTEND_STARTUP_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (isFrontendRunning()) return;
            Thread.sleep(1000);
        }
        throw new RuntimeException("Frontend did not start within " + FRONTEND_STARTUP_TIMEOUT_MS + "ms");
    }

    private static void stopFrontend() {
        if (frontendProcess != null) {
            frontendProcess.destroyForcibly();
        }
    }
}
