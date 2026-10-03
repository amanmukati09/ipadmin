package com.insurancepolicyadmin.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Static validation helper methods used across the service layer.
 *
 * <p><strong>INTENTIONAL LEGACY SMELL:</strong> this is a plain static utility
 * class (private constructor, all methods {@code public static}) rather than a
 * Spring-managed, dependency-injected bean. It is called directly as
 * {@code ValidationUtils.isValidEmail(...)} etc. from {@code CustomerServiceImpl},
 * {@code BeneficiaryServiceImpl}, and {@code PaymentServiceImpl}. This is
 * intentional per the legacy-smell catalog (Requirement 16.1) and is not to be
 * refactored into an injectable component.</p>
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9()\\-\\s]{7,20}$");

    private ValidationUtils() {
        // static utility class - not instantiable
    }

    /**
     * Checks whether the given email address matches a simple regex-based
     * email format. Null or blank input is considered invalid.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Checks whether the given phone number consists only of digits, dashes,
     * parentheses, and spaces, within a reasonable overall length. Null or
     * blank input is considered invalid.
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        String trimmed = phone.trim();
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        long digitCount = trimmed.chars().filter(Character::isDigit).count();
        return digitCount >= 7 && digitCount <= 15;
    }

    /**
     * Checks that the given date is not after today's date. Null input is
     * considered invalid (not a non-future date).
     */
    public static boolean isNotFutureDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        return !date.isAfter(LocalDate.now());
    }
}
