package com.lawlite.covid.data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.lawlite.covid.model.CovidSnapshot;
import com.lawlite.covid.model.LocationStats;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Parses the JHU CSSE global time-series CSV format.
 *
 * <p>The format has one row per location and one column per day:
 * <pre>
 * Province/State,Country/Region,Lat,Long,1/22/20,1/23/20,...,3/9/23
 * ,Afghanistan,33.93911,67.709953,0,0,...,209451
 * </pre>
 * Only the last two date columns are needed: the latest cumulative total and the
 * previous day's, from which the daily change is derived.
 */
public final class CsseCsvParser {

    static final String REGION_COLUMN = "Province/State";
    static final String COUNTRY_COLUMN = "Country/Region";

    private static final DateTimeFormatter HEADER_DATE = DateTimeFormatter.ofPattern("M/d/yy", Locale.ROOT);

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .get();

    private CsseCsvParser() {
    }

    /**
     * Parses {@code csv} into a snapshot.
     *
     * @throws InvalidDataException if the content is not in the expected format
     */
    public static CovidSnapshot parse(String csv, Instant fetchedAt) {
        String content = csv.startsWith("﻿") ? csv.substring(1) : csv;

        try (CSVParser parser = CSVParser.parse(content, FORMAT)) {
            List<String> header = parser.getHeaderNames();
            requireColumn(header, REGION_COLUMN);
            requireColumn(header, COUNTRY_COLUMN);

            int latestColumn = header.size() - 1;
            int previousColumn = latestColumn - 1;
            LocalDate reportDate = parseHeaderDate(header, latestColumn);
            parseHeaderDate(header, previousColumn);

            List<LocationStats> locations = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    throw new InvalidDataException("Line %d has %d columns, expected %d"
                            .formatted(parser.getCurrentLineNumber(), record.size(), header.size()));
                }
                String country = record.get(COUNTRY_COLUMN);
                long latest = parseCount(record, latestColumn, header, country);
                long previous = parseCount(record, previousColumn, header, country);
                locations.add(new LocationStats(record.get(REGION_COLUMN), country, latest, latest - previous));
            }

            if (locations.isEmpty()) {
                throw new InvalidDataException("The data contains a header but no rows");
            }
            return CovidSnapshot.of(reportDate, fetchedAt, locations);
        }
        catch (IOException | UncheckedIOException | IllegalArgumentException ex) {
            throw new InvalidDataException("The data is not valid CSV: " + ex.getMessage(), ex);
        }
    }

    private static void requireColumn(List<String> header, String column) {
        if (!header.contains(column)) {
            throw new InvalidDataException("Missing required column '%s'".formatted(column));
        }
    }

    private static LocalDate parseHeaderDate(List<String> header, int column) {
        if (column < 0 || column >= header.size()) {
            throw new InvalidDataException("Expected at least two date columns");
        }
        String name = header.get(column);
        try {
            return LocalDate.parse(name, HEADER_DATE);
        }
        catch (DateTimeParseException ex) {
            throw new InvalidDataException(
                    "Expected a date column in M/d/yy format but found '%s'".formatted(name), ex);
        }
    }

    /** Blank cells are treated as zero cases; anything else must be a whole number. */
    private static long parseCount(CSVRecord record, int column, List<String> header, String country) {
        String value = record.get(column);
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Invalid case count '%s' for %s on %s"
                    .formatted(value, country, header.get(column)), ex);
        }
    }
}
