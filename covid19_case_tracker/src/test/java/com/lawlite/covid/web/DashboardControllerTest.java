package com.lawlite.covid.web;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.lawlite.covid.TestData;
import com.lawlite.covid.config.TrackerProperties;
import com.lawlite.covid.model.CovidSnapshot;
import com.lawlite.covid.model.LocationStats;
import com.lawlite.covid.service.CovidDataService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DashboardController.class)
@Import({ ViewFormats.class, DashboardControllerTest.Config.class })
class DashboardControllerTest {

    /** Two days after the sample's report date, so the data is fresh by default. */
    static final Instant NOW = Instant.parse("2023-03-11T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MutableClock clock;

    @MockitoBean
    private CovidDataService dataService;

    @Test
    void showsEmptyStateBeforeDataIsLoaded() throws Exception {
        when(dataService.currentSnapshot()).thenReturn(Optional.empty());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("Data is on its way")))
                .andExpect(content().string(not(containsString("All countries"))));
    }

    @Test
    void showsHeadlineFigures() throws Exception {
        when(dataService.currentSnapshot()).thenReturn(Optional.of(TestData.sampleSnapshot()));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("62,230,596")))
                .andExpect(content().string(containsString("+40,765")))
                .andExpect(content().string(containsString("9 March 2023")))
                .andExpect(content().string(containsString("8 reporting locations")));
    }

    @Test
    void listsCountriesWithRegions() throws Exception {
        when(dataService.currentSnapshot()).thenReturn(Optional.of(TestData.sampleSnapshot()));

        mvc.perform(get("/"))
                .andExpect(content().string(containsString("Korea, South")))
                .andExpect(content().string(containsString("Côte d&#39;Ivoire")))
                .andExpect(content().string(containsString("New South Wales")))
                .andExpect(content().string(containsString("2 regions")))
                .andExpect(content().string(containsString("Mainland")))
                .andExpect(content().string(containsString("Revised down by the data source")));
    }

    @Test
    void flagsStaleData() throws Exception {
        when(dataService.currentSnapshot()).thenReturn(Optional.of(TestData.sampleSnapshot()));

        mvc.perform(get("/")).andExpect(content().string(not(containsString("Historical data"))));

        clock.set(Instant.parse("2026-10-03T00:00:00Z"));
        try {
            mvc.perform(get("/")).andExpect(content().string(containsString("Historical data")));
        }
        finally {
            clock.set(NOW);
        }
    }

    @Test
    void escapesNamesFromTheDataSource() throws Exception {
        CovidSnapshot snapshot = CovidSnapshot.of(LocalDate.of(2023, 3, 9), NOW,
                List.of(new LocationStats("", "<script>alert(1)</script>", 10, 1)));
        when(dataService.currentSnapshot()).thenReturn(Optional.of(snapshot));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")));
    }

    @TestConfiguration
    @EnableConfigurationProperties(TrackerProperties.class)
    static class Config {

        @Bean
        MutableClock clock() {
            return new MutableClock(NOW);
        }
    }

    /** A clock tests can move, so one cached application context can cover several dates. */
    static final class MutableClock extends Clock {

        private volatile Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
