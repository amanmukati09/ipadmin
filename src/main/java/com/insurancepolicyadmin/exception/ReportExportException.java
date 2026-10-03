package com.insurancepolicyadmin.exception;

/**
 * Thrown when exporting a report (e.g. the Agent book-of-business export) fails due to an
 * underlying checked exception (IOException, ParseException, MessagingException), each
 * rethrown as this unchecked exception.
 * Validates: Requirements 13.2.
 */
public class ReportExportException extends ApplicationException {

    public ReportExportException(String message) {
        super("REPORT_EXPORT_FAILED", 500, message);
    }

    public ReportExportException(String message, Throwable cause) {
        super("REPORT_EXPORT_FAILED", 500, message);
        if (cause != null) {
            super.initCause(cause);
        }
    }
}
