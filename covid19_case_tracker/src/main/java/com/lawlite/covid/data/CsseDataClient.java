package com.lawlite.covid.data;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.lawlite.covid.config.TrackerProperties;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Downloads the raw time-series CSV.
 */
@Component
public class CsseDataClient {

    private final RestClient restClient;
    private final URI dataUrl;

    public CsseDataClient(RestClient trackerRestClient, TrackerProperties properties) {
        this.restClient = trackerRestClient;
        this.dataUrl = properties.dataUrl();
    }

    /**
     * Fetches the CSV body as UTF-8 text.
     *
     * @throws org.springframework.web.client.RestClientException on connection failures or non-2xx responses
     * @throws InvalidDataException if the response has no body
     */
    public String fetchCsv() {
        // Read raw bytes and decode explicitly: the file contains non-ASCII names
        // (e.g. "Côte d'Ivoire") and not every host declares a charset.
        byte[] body = restClient.get().uri(dataUrl).retrieve().body(byte[].class);
        if (body == null || body.length == 0) {
            throw new InvalidDataException("Empty response from " + dataUrl);
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    public URI dataUrl() {
        return dataUrl;
    }
}
