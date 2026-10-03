package com.insurancepolicyadmin.dto.request;

/**
 * Request DTO for {@code POST /api/renewal-notices}.
 *
 * <p>Carries the lookahead window (in days) used to select active Policies whose current term
 * end date falls within the window from the current date (REST table endpoint 12).</p>
 */
public class RenewalNoticeRequest {
    private int lookaheadDays;

    public RenewalNoticeRequest() {
    }

    public int getLookaheadDays() {
        return lookaheadDays;
    }

    public void setLookaheadDays(int lookaheadDays) {
        this.lookaheadDays = lookaheadDays;
    }
}
