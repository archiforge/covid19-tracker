package com.lawlite.covid.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.lawlite.covid.config.TrackerProperties;
import com.lawlite.covid.data.CsseCsvParser;
import com.lawlite.covid.data.CsseDataClient;
import com.lawlite.covid.model.CovidSnapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Keeps the most recent {@link CovidSnapshot} in memory and refreshes it on a schedule.
 *
 * <p>A failed refresh never discards data that is already loaded: the previous snapshot
 * keeps being served until a later refresh succeeds.
 */
@Service
public class CovidDataService {

    private static final Logger log = LoggerFactory.getLogger(CovidDataService.class);

    private final CsseDataClient client;
    private final TrackerProperties properties;
    private final Clock clock;

    private volatile CovidSnapshot snapshot;

    public CovidDataService(CsseDataClient client, TrackerProperties properties, Clock clock) {
        this.client = client;
        this.properties = properties;
        this.clock = clock;
    }

    /** The most recently loaded data, or empty if nothing has loaded successfully yet. */
    public Optional<CovidSnapshot> currentSnapshot() {
        return Optional.ofNullable(snapshot);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        if (properties.loadOnStartup()) {
            refresh();
        }
    }

    @Scheduled(cron = "${tracker.refresh-cron}")
    public void scheduledRefresh() {
        refresh();
    }

    /** Recovers quickly when the startup download fails, instead of waiting for the next cron run. */
    @Scheduled(initialDelayString = "${tracker.retry-interval}", fixedDelayString = "${tracker.retry-interval}")
    public void retryIfNothingLoaded() {
        if (snapshot == null && properties.loadOnStartup()) {
            log.info("No COVID-19 data loaded yet, retrying");
            refresh();
        }
    }

    /**
     * Downloads and parses the data, replacing the current snapshot on success.
     *
     * @return {@code true} if new data was loaded
     */
    public synchronized boolean refresh() {
        Instant started = clock.instant();
        try {
            CovidSnapshot loaded = CsseCsvParser.parse(client.fetchCsv(), clock.instant());
            snapshot = loaded;
            log.info("Loaded {} locations across {} countries, report date {} ({} ms)",
                    loaded.locations().size(), loaded.countries().size(), loaded.reportDate(),
                    Duration.between(started, clock.instant()).toMillis());
            return true;
        }
        catch (RuntimeException ex) {
            // This is the boundary of a background job: report and keep serving what we have.
            if (snapshot == null) {
                log.warn("Could not load COVID-19 data from {}: {}", client.dataUrl(), ex.toString());
            }
            else {
                log.warn("Could not refresh COVID-19 data from {}, still serving data from {}: {}",
                        client.dataUrl(), snapshot.reportDate(), ex.toString());
            }
            log.debug("Refresh failure", ex);
            return false;
        }
    }
}
