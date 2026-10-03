package com.lawlite.covid.model;

import java.util.List;

/**
 * Confirmed-case figures for a country, summed across all of its regions.
 *
 * @param country    country or region name as published by the source
 * @param totalCases cumulative confirmed cases on the report date
 * @param newCases   change since the previous report
 * @param regions    the individual source rows, largest first; only populated when the
 *                   source breaks the country down into more than one row
 */
public record CountryStats(String country, long totalCases, long newCases, List<LocationStats> regions) {

    public CountryStats {
        regions = List.copyOf(regions);
    }

    public boolean hasRegions() {
        return !regions.isEmpty();
    }
}
