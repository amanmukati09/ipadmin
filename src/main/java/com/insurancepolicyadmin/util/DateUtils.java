package com.insurancepolicyadmin.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Static date/term-length helper methods used by {@code PremiumService} and
 * {@code EndorsementService}.
 *
 * <p>These helpers mirror the day-count and term-length arithmetic already
 * implemented in the {@code sp_endorsement_calculate_proration} and
 * {@code sp_premium_schedule_generate_installments} stored procedures
 * (see {@code db/procedures_endorsement_premium.sql}), so the Java service
 * layer and the database layer compute term lengths and day counts
 * identically.</p>
 */
public final class DateUtils {

    private DateUtils() {
        // static utility class - not instantiable
    }

    /**
     * Returns the number of days between {@code start} (inclusive) and
     * {@code end} (exclusive), equivalent to the SQL {@code DATEDIFF(end, start)}
     * used by the stored procedure layer. The result is negative if
     * {@code end} is before {@code start}.
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end dates must not be null");
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Returns the total number of days in a Policy's term, i.e. the number of
     * days between {@code termStartDate} and {@code termEndDate}. Mirrors the
     * {@code totalTermDays = DATEDIFF(term_end_date, term_start_date)}
     * computation in {@code sp_endorsement_calculate_proration}.
     */
    public static long totalTermDays(LocalDate termStartDate, LocalDate termEndDate) {
        return daysBetween(termStartDate, termEndDate);
    }

    /**
     * Returns the number of Installments a Premium_Schedule must contain for
     * the given billing frequency: monthly = 12, quarterly = 4,
     * semi_annual = 2, annual = 1. Mirrors the {@code installmentCount}
     * lookup in {@code sp_premium_schedule_generate_installments}.
     *
     * @throws IllegalArgumentException if {@code billingFrequency} is not one
     *         of the four recognized values
     */
    public static int installmentCountForFrequency(String billingFrequency) {
        if (billingFrequency == null) {
            throw new IllegalArgumentException("billingFrequency must not be null");
        }
        switch (billingFrequency) {
            case "monthly":
                return 12;
            case "quarterly":
                return 4;
            case "semi_annual":
                return 2;
            case "annual":
                return 1;
            default:
                throw new IllegalArgumentException("Unrecognized billing frequency: " + billingFrequency);
        }
    }

    /**
     * Returns the number of months between successive Installment due dates
     * for the given billing frequency: monthly = 1, quarterly = 3,
     * semi_annual = 6, annual = 12. Mirrors the {@code monthOffset} lookup in
     * {@code sp_premium_schedule_generate_installments}.
     *
     * @throws IllegalArgumentException if {@code billingFrequency} is not one
     *         of the four recognized values
     */
    public static int monthOffsetForFrequency(String billingFrequency) {
        if (billingFrequency == null) {
            throw new IllegalArgumentException("billingFrequency must not be null");
        }
        switch (billingFrequency) {
            case "monthly":
                return 1;
            case "quarterly":
                return 3;
            case "semi_annual":
                return 6;
            case "annual":
                return 12;
            default:
                throw new IllegalArgumentException("Unrecognized billing frequency: " + billingFrequency);
        }
    }
}
