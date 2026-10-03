package com.lawlite.covid.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable, fully aggregated view of one download of the source data set.
 *
 * <p>Aggregates are computed once, when the snapshot is created, so that reads from
 * concurrent web requests are cheap and always see a consistent set of figures.
 *
 * @param reportDate the date of the most recent column in the source data
 * @param fetchedAt  when the data was downloaded
 * @param locations  every source row, in source order
 * @param countries  per-country totals, largest first
 * @param totalCases global cumulative confirmed cases
 * @param newCases   global change since the previous report
 */
public record CovidSnapshot(
        LocalDate reportDate,
        Instant fetchedAt,
        List<LocationStats> locations,
        List<CountryStats> countries,
        long totalCases,
        long newCases) {

    private static final Comparator<CountryStats> LARGEST_FIRST =
            Comparator.comparingLong(CountryStats::totalCases).reversed()
                    .thenComparing(CountryStats::country);

    private static final Comparator<LocationStats> LARGEST_REGION_FIRST =
            Comparator.comparingLong(LocationStats::totalCases).reversed()
                    .thenComparing(LocationStats::region);

    public CovidSnapshot {
        locations = List.copyOf(locations);
        countries = List.copyOf(countries);
    }

    /**
     * Builds a snapshot from raw source rows, computing all aggregates.
     */
    public static CovidSnapshot of(LocalDate reportDate, Instant fetchedAt, List<LocationStats> locations) {
        Map<String, List<LocationStats>> byCountry = new LinkedHashMap<>();
        for (LocationStats location : locations) {
            byCountry.computeIfAbsent(location.country(), country -> new ArrayList<>()).add(location);
        }

        List<CountryStats> countries = byCountry.entrySet().stream()
                .map(entry -> toCountryStats(entry.getKey(), entry.getValue()))
                .sorted(LARGEST_FIRST)
                .toList();

        long totalCases = countries.stream().mapToLong(CountryStats::totalCases).sum();
        long newCases = countries.stream().mapToLong(CountryStats::newCases).sum();
        return new CovidSnapshot(reportDate, fetchedAt, locations, countries, totalCases, newCases);
    }

    private static CountryStats toCountryStats(String country, List<LocationStats> rows) {
        long total = rows.stream().mapToLong(LocationStats::totalCases).sum();
        long added = rows.stream().mapToLong(LocationStats::newCases).sum();
        List<LocationStats> regions = rows.size() > 1
                ? rows.stream().sorted(LARGEST_REGION_FIRST).toList()
                : List.of();
        return new CountryStats(country, total, added, regions);
    }

    /** The {@code limit} countries with the most cumulative cases. */
    public List<CountryStats> topCountries(int limit) {
        return countries.stream().limit(limit).toList();
    }

    /** The {@code limit} countries with the largest increase since the previous report. */
    public List<CountryStats> fastestGrowing(int limit) {
        return countries.stream()
                .filter(country -> country.newCases() > 0)
                .sorted(Comparator.comparingLong(CountryStats::newCases).reversed()
                        .thenComparing(CountryStats::country))
                .limit(limit)
                .toList();
    }

    /** Percentage of global cases that {@code cases} represents, from 0 to 100. */
    public double shareOfTotal(long cases) {
        return totalCases == 0 ? 0 : cases * 100.0 / totalCases;
    }
}
