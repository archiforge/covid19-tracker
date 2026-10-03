package com.lawlite.covid.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under the {@code tracker.*} prefix.
 *
 * <p>The refresh schedule ({@code tracker.refresh-cron}) and retry interval
 * ({@code tracker.retry-interval}) are read directly by {@code @Scheduled}, so they live
 * in {@code application.properties} rather than here.
 *
 * @param dataUrl         CSV in the JHU CSSE "time_series_covid19_*_global" format
 * @param loadOnStartup   whether to download the data as soon as the application is ready
 * @param connectTimeout  maximum time to establish a connection to {@code dataUrl}
 * @param readTimeout     maximum time to wait for the download to complete
 * @param staleAfter      how old the report date may be before the dashboard flags the data as stale
 */
@ConfigurationProperties("tracker")
public record TrackerProperties(
        @DefaultValue("https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv")
        URI dataUrl,
        @DefaultValue("true") boolean loadOnStartup,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("60s") Duration readTimeout,
        @DefaultValue("7d") Duration staleAfter) {
}
