package com.lawlite.covid;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the whole application on a random port, with no data loaded.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = { "tracker.load-on-startup=false", "tracker.refresh-cron=-" })
class CovidTrackerApplicationTests {

    private final HttpClient http = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    @Value("${local.server.port}")
    private int port;

    @Test
    void dashboardShowsEmptyStateBeforeDataIsLoaded() throws Exception {
        HttpResponse<String> response = get("/", "text/html");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Data is on its way");
    }

    @Test
    void staticAssetsAreFingerprintedAndCachedLongTerm() throws Exception {
        String page = get("/", "text/html").body();
        Matcher stylesheet = Pattern.compile("href=\"(/css/app-[0-9a-f]{32}\\.css)\"").matcher(page);
        assertThat(stylesheet.find()).as("fingerprinted stylesheet link").isTrue();

        HttpResponse<String> css = get(stylesheet.group(1), "text/css");

        assertThat(css.statusCode()).isEqualTo(200);
        assertThat(css.headers().firstValue("Cache-Control")).hasValueSatisfying(
                value -> assertThat(value).contains("max-age=31536000"));
    }

    @Test
    void unknownPagesRenderTheErrorPage() throws Exception {
        HttpResponse<String> response = get("/no-such-page", "text/html");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Page not found").contains("Back to the dashboard");
    }

    @Test
    void apiReportsUnavailableBeforeDataIsLoaded() throws Exception {
        HttpResponse<String> response = get("/api/summary", "application/json");

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
    }

    private HttpResponse<String> get(String path, String accept) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", accept)
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
