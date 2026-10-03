package com.lawlite.covid;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.lawlite.covid.data.CsseCsvParser;
import com.lawlite.covid.model.CovidSnapshot;

import org.springframework.core.io.ClassPathResource;

/**
 * Shared access to {@code data/time_series_sample.csv}.
 *
 * <p>Expected figures for the sample, report date 2023-03-09:
 * <pre>
 * Country          Total        New
 * Korea, South     30,610,335   +10,335
 * United Kingdom   24,443,639   +28,779   (mainland + Bermuda)
 * Australia         6,861,650    +1,550   (New South Wales + Victoria)
 * Afghanistan         209,451      +111
 * Côte d'Ivoire        88,331         0
 * Monaco               17,190       -10   (revised down)
 * -------------------------------------
 * Global           62,230,596   +40,765   6 countries, 8 locations
 * </pre>
 */
public final class TestData {

    public static final Instant FETCHED_AT = Instant.parse("2023-03-10T10:00:00Z");

    private TestData() {
    }

    public static String sampleCsv() {
        try {
            return new ClassPathResource("data/time_series_sample.csv").getContentAsString(StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static CovidSnapshot sampleSnapshot() {
        return CsseCsvParser.parse(sampleCsv(), FETCHED_AT);
    }
}
