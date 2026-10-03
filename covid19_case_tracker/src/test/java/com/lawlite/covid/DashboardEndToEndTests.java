package com.lawlite.covid;

import com.lawlite.covid.data.CsseDataClient;
import com.lawlite.covid.service.CovidDataService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real parser, service, controllers and templates against the sample CSV,
 * replacing only the network download.
 */
@SpringBootTest(properties = { "tracker.load-on-startup=false", "tracker.refresh-cron=-" })
@AutoConfigureMockMvc
class DashboardEndToEndTests {

    @MockitoBean
    private CsseDataClient client;

    @Autowired
    private CovidDataService dataService;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void loadSampleData() {
        when(client.fetchCsv()).thenReturn(TestData.sampleCsv());
        assertThat(dataService.refresh()).isTrue();
    }

    @Test
    void dashboardRendersSampleData() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("62,230,596")))
                .andExpect(content().string(containsString("Korea, South")))
                .andExpect(content().string(containsString("Most confirmed cases")));
    }

    @Test
    void apiServesSampleData() throws Exception {
        mvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(62_230_596))
                .andExpect(jsonPath("$.reportDate").value("2023-03-09"));
    }
}
