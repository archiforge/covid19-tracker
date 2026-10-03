package com.lawlite.covid.data;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.lawlite.covid.config.TrackerProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CsseDataClientTest {

    private static final URI DATA_URL = URI.create("https://example.org/confirmed.csv");

    private MockRestServiceServer server;
    private CsseDataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        TrackerProperties properties = new TrackerProperties(DATA_URL, true,
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofDays(7));
        client = new CsseDataClient(builder.build(), properties);
    }

    @Test
    void decodesBodyAsUtf8EvenWithoutDeclaredCharset() {
        byte[] body = "Province/State,Country/Region\n,Côte d'Ivoire\n".getBytes(StandardCharsets.UTF_8);
        server.expect(requestTo(DATA_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.TEXT_PLAIN));

        assertThat(client.fetchCsv()).contains("Côte d'Ivoire");
        server.verify();
    }

    @Test
    void propagatesServerErrors() {
        server.expect(requestTo(DATA_URL)).andRespond(withServerError());

        assertThatThrownBy(client::fetchCsv).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void rejectsEmptyResponses() {
        server.expect(requestTo(DATA_URL)).andRespond(withSuccess());

        assertThatThrownBy(client::fetchCsv)
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("Empty response");
    }
}
