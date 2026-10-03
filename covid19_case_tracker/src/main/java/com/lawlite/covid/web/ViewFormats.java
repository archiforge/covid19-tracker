package com.lawlite.covid.web;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Number and date formatting for templates, available as {@code ${@formats.integer(value)}}.
 *
 * <p>{@link NumberFormat} instances are not thread-safe, so each call creates its own.
 */
@Component("formats")
public class ViewFormats {

    private static final Locale LOCALE = Locale.ENGLISH;

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", LOCALE);
    private static final DateTimeFormatter DATE_TIME_UTC =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'", LOCALE).withZone(ZoneOffset.UTC);

    /** {@code 1234567} becomes {@code "1,234,567"}. */
    public String integer(long value) {
        return NumberFormat.getIntegerInstance(LOCALE).format(value);
    }

    /** Adds an explicit sign to non-zero values: {@code "+1,234"}, {@code "−56"}, {@code "0"}. */
    public String signed(long value) {
        if (value > 0) {
            return "+" + integer(value);
        }
        if (value < 0) {
            return "−" + integer(-value);
        }
        return "0";
    }

    /** {@code 103802702} becomes {@code "103.8M"}. */
    public String compact(long value) {
        NumberFormat format = NumberFormat.getCompactNumberInstance(LOCALE, NumberFormat.Style.SHORT);
        format.setMaximumFractionDigits(1);
        return format.format(value);
    }

    /** A 0–100 percentage with one decimal place; small non-zero shares show as {@code "<0.1%"}. */
    public String percent(double value) {
        if (value > 0 && value < 0.05) {
            return "<0.1%";
        }
        NumberFormat format = NumberFormat.getNumberInstance(LOCALE);
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        return format.format(value) + "%";
    }

    /** A 0–1 ratio for CSS custom properties, always with a '.' decimal separator. */
    public String ratio(long value, long max) {
        double ratio = max <= 0 ? 0 : Math.clamp((double) value / max, 0.0, 1.0);
        return String.format(Locale.ROOT, "%.4f", ratio);
    }

    /** {@code "9 Mar 2023"}. */
    public String shortDate(LocalDate date) {
        return SHORT_DATE.format(date);
    }

    /** {@code "9 March 2023"}. */
    public String longDate(LocalDate date) {
        return LONG_DATE.format(date);
    }

    /** {@code "3 Oct 2026, 05:20 UTC"}. */
    public String dateTime(Instant instant) {
        return DATE_TIME_UTC.format(instant);
    }
}
