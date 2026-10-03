package com.lawlite.covid.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.lawlite.covid.model.CountryStats;
import com.lawlite.covid.model.CovidSnapshot;
import com.lawlite.covid.service.CovidDataService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * JSON access to the same figures shown on the dashboard.
 */
@RestController
@RequestMapping("/api")
public class StatsApiController {

    private final CovidDataService dataService;

    public StatsApiController(CovidDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/summary")
    public Summary summary() {
        CovidSnapshot snapshot = requireSnapshot();
        return new Summary(snapshot.reportDate(), snapshot.fetchedAt(), snapshot.totalCases(),
                snapshot.newCases(), snapshot.countries().size(), snapshot.locations().size());
    }

    @GetMapping("/countries")
    public List<CountryStats> countries() {
        return requireSnapshot().countries();
    }

    @GetMapping("/countries/{country}")
    public CountryStats country(@PathVariable String country) {
        return requireSnapshot().countries().stream()
                .filter(stats -> stats.country().equalsIgnoreCase(country.strip()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No data for country '%s'".formatted(country)));
    }

    private CovidSnapshot requireSnapshot() {
        return dataService.currentSnapshot().orElseThrow(DataNotLoadedException::new);
    }

    /**
     * Headline figures.
     *
     * @param countries number of countries in the data
     * @param locations number of rows in the data, counting each province/state separately
     */
    public record Summary(LocalDate reportDate, Instant fetchedAt, long totalCases, long newCases,
            int countries, int locations) {
    }
}
