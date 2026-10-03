package com.lawlite.covid.web;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViewFormatsTest {

    private final ViewFormats formats = new ViewFormats();

    @Test
    void formatsIntegersWithGrouping() {
        assertThat(formats.integer(676_570_149)).isEqualTo("676,570,149");
        assertThat(formats.integer(0)).isEqualTo("0");
    }

    @Test
    void formatsSignedValues() {
        assertThat(formats.signed(46_931)).isEqualTo("+46,931");
        assertThat(formats.signed(-10)).isEqualTo("−10");
        assertThat(formats.signed(0)).isEqualTo("0");
    }

    @Test
    void formatsCompactValues() {
        assertThat(formats.compact(103_802_702)).isEqualTo("103.8M");
        assertThat(formats.compact(46_931)).isEqualTo("46.9K");
        assertThat(formats.compact(512)).isEqualTo("512");
    }

    @Test
    void formatsPercentages() {
        assertThat(formats.percent(15.3456)).isEqualTo("15.3%");
        assertThat(formats.percent(100)).isEqualTo("100.0%");
        assertThat(formats.percent(0.01)).isEqualTo("<0.1%");
        assertThat(formats.percent(0)).isEqualTo("0.0%");
    }

    @Test
    void formatsRatiosForCss() {
        assertThat(formats.ratio(50, 200)).isEqualTo("0.2500");
        assertThat(formats.ratio(300, 200)).isEqualTo("1.0000");
        assertThat(formats.ratio(-5, 200)).isEqualTo("0.0000");
        assertThat(formats.ratio(5, 0)).isEqualTo("0.0000");
    }

    @Test
    void formatsDates() {
        assertThat(formats.shortDate(LocalDate.of(2023, 3, 9))).isEqualTo("9 Mar 2023");
        assertThat(formats.longDate(LocalDate.of(2023, 3, 9))).isEqualTo("9 March 2023");
        assertThat(formats.dateTime(Instant.parse("2026-10-03T05:20:59Z"))).isEqualTo("3 Oct 2026, 05:20 UTC");
    }
}
