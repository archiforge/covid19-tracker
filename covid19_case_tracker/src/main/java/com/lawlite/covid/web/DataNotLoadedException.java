package com.lawlite.covid.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Rendered as a 503 problem response when an API call arrives before the first
 * successful data load.
 */
class DataNotLoadedException extends ErrorResponseException {

    static final String RETRY_AFTER_SECONDS = "60";

    DataNotLoadedException() {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                        "COVID-19 data has not been loaded yet. Try again shortly."),
                null);
        getHeaders().set(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS);
    }
}
