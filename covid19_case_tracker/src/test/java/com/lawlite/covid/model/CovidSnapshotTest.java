package com.lawlite.covid.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.lawlite.covid.TestData;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

class CovidSnapshotTest {

    private final CovidSnapshot snapshot = TestData.sampleSnapshot();

    @Test
    void computesGlobalTotals() {
        assertThat(snapshot.totalCases()).isEqualTo(62_230_596);
        assertThat(snapshot.newCases()).isEqualTo(40_765);
    }

    @Test
    void aggregatesRowsByCountryLargestFirst() {
        assertThat(snapshot.countries())
                .extracting(CountryStats::country, CountryStats::totalCases, CountryStats::newCases)
                .containsExactly(
                        tuple("Korea, South", 30_610_335L, 10_335L),
                        tuple("United Kingdom", 24_443_639L, 28_779L),
                        tuple("Australia", 6_861_650L, 1_550L),
                        tuple("Afghanistan", 209_451L, 111L),
                        tuple("Côte d'Ivoire", 88_331L, 0L),
                        tuple("Monaco", 17_190L, -10L));
    }

    @Test
    void listsRegionsOnlyForCountriesWithSeveralRowsLargestFirst() {
        CountryStats unitedKingdom = country("United Kingdom");
        CountryStats afghanistan = country("Afghanistan");

        assertThat(unitedKingdom.hasRegions()).isTrue();
        assertThat(unitedKingdom.regions()).extracting(LocationStats::region).containsExactly("", "Bermuda");
        assertThat(afghanistan.hasRegions()).isFalse();
        assertThat(afghanistan.regions()).isEmpty();
    }

    @Test
    void topCountriesAreTheLargestByTotal() {
        assertThat(snapshot.topCountries(2)).extracting(CountryStats::country)
                .containsExactly("Korea, South", "United Kingdom");
        assertThat(snapshot.topCountries(100)).hasSize(6);
    }

    @Test
    void fastestGrowingExcludesCountriesWithoutNewCases() {
        assertThat(snapshot.fastestGrowing(10)).extracting(CountryStats::country)
                .containsExactly("United Kingdom", "Korea, South", "Australia", "Afghanistan");
    }

    @Test
    void shareOfTotalIsAPercentage() {
        assertThat(snapshot.shareOfTotal(snapshot.totalCases())).isEqualTo(100.0);
        assertThat(snapshot.shareOfTotal(30_610_335)).isCloseTo(49.19, within(0.01));
    }

    @Test
    void shareOfEmptyTotalIsZero() {
        CovidSnapshot empty = CovidSnapshot.of(LocalDate.of(2023, 3, 9), Instant.EPOCH,
                List.of(new LocationStats(null, "Atlantis", 0, 0)));

        assertThat(empty.shareOfTotal(0)).isZero();
    }

    @Test
    void isImmutable() {
        List<LocationStats> rows = new ArrayList<>(snapshot.locations());
        CovidSnapshot copy = CovidSnapshot.of(snapshot.reportDate(), snapshot.fetchedAt(), rows);
        rows.clear();

        assertThat(copy.locations()).hasSize(8);
    }

    private CountryStats country(String name) {
        return snapshot.countries().stream().filter(c -> c.country().equals(name)).findFirst().orElseThrow();
    }
}
