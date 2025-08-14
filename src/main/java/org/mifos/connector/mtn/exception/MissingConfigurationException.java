package org.mifos.connector.mtn.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a required configuration is missing in the MTN connector.
 *
 */
public class MissingConfigurationException extends MtnConnectorException {

    public MissingConfigurationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public MissingConfigurationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_REQUEST);
    }
}
