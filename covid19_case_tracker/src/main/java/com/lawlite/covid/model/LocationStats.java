package com.lawlite.covid.model;

import java.util.Objects;

/**
 * Confirmed-case figures for a single row of the source data set: a country, or a
 * province/state/territory within a country.
 *
 * @param region     province, state or territory; empty when the row covers the whole country
 *                   (or, for countries that also list overseas territories, its mainland)
 * @param country    country or region name as published by the source
 * @param totalCases cumulative confirmed cases on the report date
 * @param newCases   change since the previous report; can be negative when the source
 *                   corrected earlier figures
 */
public record LocationStats(String region, String country, long totalCases, long newCases) {

    public LocationStats {
        region = Objects.requireNonNullElse(region, "").strip();
        country = Objects.requireNonNull(country, "country").strip();
    }

    public boolean hasRegion() {
        return !region.isEmpty();
    }
}
