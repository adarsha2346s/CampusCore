package com.campuscore.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Payload for correcting a recorded mark.
 *
 * <p>Only the awarded value is editable. The enrollment and the assessment are
 * not: they identify which student's result this row belongs to and which
 * assessment it measures. Re-pointing a grade would either collide with the
 * one-mark-per-enrollment-per-assessment constraint or quietly move a score onto
 * the wrong assessment, so a misfiled mark is re-entered instead.
 *
 * <p>The upper bound against the assessment's maximum marks is enforced in the
 * service, because the maximum is held on the assessment rather than in this
 * payload.
 */
public class MarkRequest {

    @NotNull(message = "Marks obtained is required")
    @DecimalMin(value = "0.00", message = "Marks cannot be negative")
    @Digits(integer = 4, fraction = 2, message = "Marks allows up to two decimals")
    private BigDecimal marksObtained;

    public MarkRequest() {
    }

    public MarkRequest(BigDecimal marksObtained) {
        this.marksObtained = marksObtained;
    }

    public BigDecimal getMarksObtained() {
        return marksObtained;
    }

    public void setMarksObtained(BigDecimal marksObtained) {
        this.marksObtained = marksObtained;
    }
}