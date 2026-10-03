package com.insurancepolicyadmin.exception;

/**
 * Thrown when generation (or regeneration) of a Policy's Premium_Schedule/Installments fails,
 * e.g. during binding (Requirement 7.8) or Endorsement-triggered regeneration (Requirement 9.10).
 * Validates: Requirements 7.6, 7.8, 9.10, 10.4.
 */
public class ScheduleGenerationException extends ApplicationException {

    public ScheduleGenerationException(String message) {
        super("SCHEDULE_GENERATION_FAILED", 500, message);
    }

    public ScheduleGenerationException(String message, Throwable cause) {
        super("SCHEDULE_GENERATION_FAILED", 500, message);
        if (cause != null) {
            super.initCause(cause);
        }
    }
}
