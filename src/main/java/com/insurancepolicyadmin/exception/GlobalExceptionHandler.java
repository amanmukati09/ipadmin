package com.insurancepolicyadmin.exception;

import com.insurancepolicyadmin.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

/**
 * Centralized exception-to-HTTP-response mapping for the whole application, per design.md's
 * "Error Handling" section. Must live in {@code com.insurancepolicyadmin.exception} AND be
 * component-scanned by {@code dispatcher-servlet.xml} (not just {@code applicationContext.xml})
 * for {@code @ControllerAdvice} to take effect - see the deployment note carried forward from
 * the prior fixture.
 *
 * <p>Every concrete exception thrown by this application (see Task 7.3) already extends
 * {@link ApplicationException} and carries its own correct {@code errorCode}/{@code httpStatus}/
 * {@code message}/{@code fieldErrors}. A single generic handler for the common base type is
 * therefore sufficient to cover every subclass - {@code PolicyNotFoundException},
 * {@code CustomerNotFoundException}, {@code ValidationException}, {@code OverpaymentException},
 * etc. - without 14 near-identical per-subclass handler methods.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException ex) {
        logger.warn("Handled application exception: errorCode={}, httpStatus={}, message={}",
                ex.getErrorCode(), ex.getHttpStatus(), ex.getMessage());
        ErrorResponse body = new ErrorResponse(
                ex.getErrorCode(), ex.getMessage(), ex.getFieldErrors(), LocalDateTime.now());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        logger.error("Unhandled exception reaching GlobalExceptionHandler", ex);
        ErrorResponse body = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                java.util.Collections.emptyList(),
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
