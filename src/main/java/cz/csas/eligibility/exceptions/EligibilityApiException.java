package cz.csas.eligibility.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Custom exception signaling failure of eligibility endpoint.
 */
@Getter
public class EligibilityApiException extends RuntimeException {
    /**
     *  CorrelationId which needs to be included into response header.
     */
    private final String correlationId;

    public EligibilityApiException(String correlationId, String message, Throwable cause) {
        super(message, cause);

        this.correlationId = correlationId;
    }

}