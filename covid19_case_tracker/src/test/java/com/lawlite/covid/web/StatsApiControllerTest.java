package com.lawlite.covid.web;

import java.util.Optional;

import com.lawlite.covid.TestData;
import com.lawlite.covid.service.CovidDataService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsApiController.class)
class StatsApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CovidDataService dataService;

    @Test
    void returnsSummary() throws Exception {
        when(dataService.currentSnapshot()).thenReturn(Optional.of(TestData.sampleSnapshot()));

        mvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reportDate").value("2023-03-09"))
                .andExpect(jsonPath("$.fetchedAt").value("2023-03-10T10:00:00Z"))
                .andExpect(jsonPath("$.totalCases").value(62_230_596))
                .andExpect(jsonPath("$.newCases").value(40_765))
                .andExpect(jsonPath("$.countries").value(6))
                .andExpect(jsonPath("$.locations").value(8));
    }

    @Test
    void returnsCountriesLargestFirst() throws Exception {
        when(dataService.currentSnapshot()).thenReturn(Optional.of(TestData.sampleSnapshot()));

        mvc.perform(get("/api/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].country").value("Korea, South"))
                .andExpect(jsonPath("$[0].totalCases").value(30_610_335))
                .andExpect(jsonPath("$[0].regions", hasSize(0)))
                .andExpect(jsonPath("$[2].country").value("Australia"))
                .andExpect(jsonPath("$[2].regions[0].region").value("New South Wales"))
                .andExpect(jsonPath("$[2].regions[0].newCases").value(1_500));
    }

    @Test
    void findsCountryIgnoringCase() throws Exception {
        when(dataService.currentSnapshot()).thenReturn(Optional.of(TestData.sampleSnapshot()));

        mvc.perform(get("/api/countries/{country}", "united kingdom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("United Kingdom"))
                .andExpect(jsonPath("$.totalCases").value(24_443_639))
                .andExpect(jsonPath("$.regions", hasSize(2)));
    }

    @Test
    void unknownCountryIsNotFound() throws Exception {
        when(dataService.currentSnapshot()).thenReturn(Optional.of(TestData.sampleSnapshot()));

        mvc.perform(get("/api/countries/{country}", "Atlantis"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No data for country 'Atlantis'"));
    }

    @Test
    void isUnavailableBeforeDataIsLoaded() throws Exception {
        when(dataService.currentSnapshot()).thenReturn(Optional.empty());

        mvc.perform(get("/api/summary"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("Service Unavailable"));
    }
}
