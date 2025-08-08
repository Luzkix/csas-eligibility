package cz.csas.eligibility.exceptions;

import cz.csas.eligibility.model.ErrorDto;
import cz.csas.eligibility.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            EligibilityApiException.class
    })
    public ResponseEntity<Object> handleType400exceptions(final Exception exception,
                                                          final WebRequest request) {

        return handleException(exception, new HttpHeaders(), BAD_REQUEST, request);
    }

    private ResponseEntity<Object> handleException(Exception e, HttpHeaders headers,
                                                   HttpStatus status, WebRequest request) {

        if ((e instanceof EligibilityApiException)) {
            try {
                headers.add("correlation-id", ((EligibilityApiException) e).getCorrelationId());
            } catch (NullPointerException ex) {
                headers.add("correlation-id", null);
            }
        }

        ErrorDto errorDto = new ErrorDto();
        errorDto.setErrorStatusValue(status.value());
        errorDto.setErrorStatus(status.name());
        errorDto.setErrorTime(DateUtils.convertToSystemOffsetDateTime(LocalDateTime.now()));
        errorDto.setErrorMessage(e.getMessage());


        log.error("An exception was thrown from a REST controller. Creating an ErrorDto (errorStatusValue: {}, errorMessage: {})",
                        errorDto.getErrorStatusValue(), e.getMessage());

        return super.handleExceptionInternal(e, errorDto, headers, status, request);
    }

}
