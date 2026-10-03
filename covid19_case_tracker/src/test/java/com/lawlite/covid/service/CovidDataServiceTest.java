package com.lawlite.covid.service;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.lawlite.covid.TestData;
import com.lawlite.covid.config.TrackerProperties;
import com.lawlite.covid.data.CsseDataClient;
import com.lawlite.covid.model.CovidSnapshot;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CovidDataServiceTest {

    private static final Instant NOW = Instant.parse("2023-03-10T10:00:00Z");

    private final CsseDataClient client = mock(CsseDataClient.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void startsEmpty() {
        assertThat(service(true).currentSnapshot()).isEmpty();
    }

    @Test
    void refreshLoadsSnapshot() {
        when(client.fetchCsv()).thenReturn(TestData.sampleCsv());
        CovidDataService service = service(true);

        assertThat(service.refresh()).isTrue();

        CovidSnapshot snapshot = service.currentSnapshot().orElseThrow();
        assertThat(snapshot.reportDate()).isEqualTo(LocalDate.of(2023, 3, 9));
        assertThat(snapshot.fetchedAt()).isEqualTo(NOW);
        assertThat(snapshot.totalCases()).isEqualTo(62_230_596);
    }

    @Test
    void failedFirstRefreshLeavesServiceEmpty() {
        when(client.fetchCsv()).thenThrow(new ResourceAccessException("Connection refused"));
        CovidDataService service = service(true);

        assertThat(service.refresh()).isFalse();
        assertThat(service.currentSnapshot()).isEmpty();
    }

    @Test
    void failedRefreshKeepsServingPreviousData() {
        when(client.fetchCsv())
                .thenReturn(TestData.sampleCsv())
                .thenThrow(new ResourceAccessException("Connection refused"));
        CovidDataService service = service(true);
        service.refresh();
        CovidSnapshot loaded = service.currentSnapshot().orElseThrow();

        assertThat(service.refresh()).isFalse();
        assertThat(service.currentSnapshot()).containsSame(loaded);
    }

    @Test
    void invalidDataKeepsServingPreviousData() {
        when(client.fetchCsv())
                .thenReturn(TestData.sampleCsv())
                .thenReturn("<html>Not a CSV</html>");
        CovidDataService service = service(true);
        service.refresh();
        CovidSnapshot loaded = service.currentSnapshot().orElseThrow();

        assertThat(service.refresh()).isFalse();
        assertThat(service.currentSnapshot()).containsSame(loaded);
    }

    @Test
    void loadsOnStartupWhenEnabled() {
        when(client.fetchCsv()).thenReturn(TestData.sampleCsv());
        CovidDataService service = service(true);

        service.loadOnStartup();

        assertThat(service.currentSnapshot()).isPresent();
    }

    @Test
    void skipsStartupLoadWhenDisabled() {
        CovidDataService service = service(false);

        service.loadOnStartup();
        service.retryIfNothingLoaded();

        verify(client, never()).fetchCsv();
        assertThat(service.currentSnapshot()).isEmpty();
    }

    @Test
    void retriesOnlyWhileNothingIsLoaded() {
        when(client.fetchCsv()).thenReturn(TestData.sampleCsv());
        CovidDataService service = service(true);

        service.retryIfNothingLoaded();
        service.retryIfNothingLoaded();

        verify(client, times(1)).fetchCsv();
    }

    private CovidDataService service(boolean loadOnStartup) {
        when(client.dataUrl()).thenReturn(URI.create("https://example.org/confirmed.csv"));
        TrackerProperties properties = new TrackerProperties(URI.create("https://example.org/confirmed.csv"),
                loadOnStartup, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofDays(7));
        return new CovidDataService(client, properties, clock);
    }
}
