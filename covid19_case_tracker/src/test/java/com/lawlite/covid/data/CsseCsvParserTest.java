package com.lawlite.covid.data;

import java.time.Instant;
import java.time.LocalDate;

import com.lawlite.covid.TestData;
import com.lawlite.covid.model.CovidSnapshot;
import com.lawlite.covid.model.LocationStats;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsseCsvParserTest {

    private static final Instant NOW = Instant.parse("2023-03-10T10:00:00Z");

    @Test
    void readsReportDateFromLastColumn() {
        CovidSnapshot snapshot = TestData.sampleSnapshot();

        assertThat(snapshot.reportDate()).isEqualTo(LocalDate.of(2023, 3, 9));
        assertThat(snapshot.fetchedAt()).isEqualTo(TestData.FETCHED_AT);
    }

    @Test
    void readsEveryRowInSourceOrder() {
        CovidSnapshot snapshot = TestData.sampleSnapshot();

        assertThat(snapshot.locations()).containsExactly(
                new LocationStats("", "Afghanistan", 209_451, 111),
                new LocationStats("New South Wales", "Australia", 3_931_500, 1_500),
                new LocationStats("Victoria", "Australia", 2_930_150, 50),
                new LocationStats("", "Korea, South", 30_610_335, 10_335),
                new LocationStats("", "Côte d'Ivoire", 88_331, 0),
                new LocationStats("Bermuda", "United Kingdom", 18_860, 0),
                new LocationStats("", "United Kingdom", 24_424_779, 28_779),
                new LocationStats("", "Monaco", 17_190, -10));
    }

    @Test
    void stripsByteOrderMark() {
        CovidSnapshot snapshot = CsseCsvParser.parse("﻿" + TestData.sampleCsv(), NOW);

        assertThat(snapshot.locations()).hasSize(8);
    }

    @Test
    void treatsBlankCellsAsZero() {
        String csv = """
                Province/State,Country/Region,Lat,Long,1/1/21,1/2/21
                ,Atlantis,0,0,,5
                """;

        assertThat(CsseCsvParser.parse(csv, NOW).locations())
                .containsExactly(new LocationStats("", "Atlantis", 5, 5));
    }

    @Test
    void ignoresBlankLines() {
        String csv = """
                Province/State,Country/Region,Lat,Long,1/1/21,1/2/21
                ,Atlantis,0,0,1,2

                """;

        assertThat(CsseCsvParser.parse(csv, NOW).locations()).hasSize(1);
    }

    @Test
    void rejectsMissingCountryColumn() {
        String csv = """
                Province/State,Lat,Long,1/1/21,1/2/21
                ,0,0,1,2
                """;

        assertThatThrownBy(() -> CsseCsvParser.parse(csv, NOW))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("Country/Region");
    }

    @Test
    void rejectsLastColumnThatIsNotADate() {
        String csv = """
                Province/State,Country/Region,Lat,Long,1/1/21,Notes
                ,Atlantis,0,0,1,none
                """;

        assertThatThrownBy(() -> CsseCsvParser.parse(csv, NOW))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("'Notes'");
    }

    @Test
    void rejectsSingleDateColumn() {
        String csv = """
                Province/State,Country/Region,1/1/21
                ,Atlantis,1
                """;

        assertThatThrownBy(() -> CsseCsvParser.parse(csv, NOW))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("date");
    }

    @Test
    void rejectsNonNumericCounts() {
        String csv = """
                Province/State,Country/Region,Lat,Long,1/1/21,1/2/21
                ,Atlantis,0,0,1,lots
                """;

        assertThatThrownBy(() -> CsseCsvParser.parse(csv, NOW))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("'lots'")
                .hasMessageContaining("Atlantis")
                .hasMessageContaining("1/2/21");
    }

    @Test
    void rejectsRowsWithMissingColumns() {
        String csv = """
                Province/State,Country/Region,Lat,Long,1/1/21,1/2/21
                ,Atlantis,0,0,1
                """;

        assertThatThrownBy(() -> CsseCsvParser.parse(csv, NOW))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("columns");
    }

    @Test
    void rejectsHeaderWithoutRows() {
        String csv = "Province/State,Country/Region,Lat,Long,1/1/21,1/2/21\n";

        assertThatThrownBy(() -> CsseCsvParser.parse(csv, NOW))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("no rows");
    }

    @Test
    void rejectsContentThatIsNotCsv() {
        assertThatThrownBy(() -> CsseCsvParser.parse("<html><body>Rate limited</body></html>", NOW))
                .isInstanceOf(InvalidDataException.class);
    }
}
