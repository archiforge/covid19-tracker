package com.lawlite.covid.web;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders API errors as RFC 9457 problem details ({@code application/problem+json}).
 *
 * <p>Scoped to the API so that browsers still get the HTML error page for everything else.
 */
@RestControllerAdvice(assignableTypes = StatsApiController.class)
class ApiExceptionHandler extends ResponseEntityExceptionHandler {
}
