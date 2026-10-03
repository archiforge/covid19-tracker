package com.lawlite.covid.web;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.lawlite.covid.config.TrackerProperties;
import com.lawlite.covid.model.CovidSnapshot;
import com.lawlite.covid.service.CovidDataService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    static final int TOP_COUNTRIES = 10;
    static final int FASTEST_GROWING = 10;

    private final CovidDataService dataService;
    private final TrackerProperties properties;
    private final Clock clock;

    public DashboardController(CovidDataService dataService, TrackerProperties properties, Clock clock) {
        this.dataService = dataService;
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        dataService.currentSnapshot().ifPresent(snapshot -> addSnapshot(model, snapshot));
        return "dashboard";
    }

    private void addSnapshot(Model model, CovidSnapshot snapshot) {
        long reportAgeDays = ChronoUnit.DAYS.between(snapshot.reportDate(), LocalDate.now(clock));
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("topCountries", snapshot.topCountries(TOP_COUNTRIES));
        model.addAttribute("fastestGrowing", snapshot.fastestGrowing(FASTEST_GROWING));
        model.addAttribute("stale", reportAgeDays > properties.staleAfter().toDays());
    }
}
