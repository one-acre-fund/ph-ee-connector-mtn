package org.mifos.connector.mtn.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Parent class for all exceptions in the MTN connector.
 *
 * @author amy.muhimpundu
 */
@Getter
public class MtnConnectorException extends RuntimeException {

    private final HttpStatus httpStatus;

    public MtnConnectorException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public MtnConnectorException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}
