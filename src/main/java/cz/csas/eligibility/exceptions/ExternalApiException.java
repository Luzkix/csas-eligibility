package cz.csas.eligibility.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Custom exception to signal external API failures.
 */
@Getter
public class ExternalApiException extends RuntimeException {

    /**
     *  HTTP status code returned by the external API.
     */
    private final HttpStatusCode statusCode;

    /**
     *  The raw response body from the external API error, if available.
     */
    private final String errorBody;

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);

        // If cause is HttpClientErrorException, extract statusCode and errorBody; else default to statusCode 500 and error message
        if (cause instanceof HttpClientErrorException httpEx) {
            this.statusCode = httpEx.getStatusCode();
            this.errorBody = httpEx.getResponseBodyAsString();
        } else {
            this.statusCode = HttpStatusCode.valueOf(500);
            this.errorBody = cause.getMessage();
        }
    }

}