package com.lawlite.covid.data;

/**
 * Thrown when downloaded data cannot be interpreted as a JHU CSSE time-series CSV.
 */
public class InvalidDataException extends RuntimeException {

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
