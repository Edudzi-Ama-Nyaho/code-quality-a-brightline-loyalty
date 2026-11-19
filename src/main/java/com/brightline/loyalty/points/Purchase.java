package com.brightline.loyalty.points;

import java.math.BigDecimal;

/** One purchase to award points for: the member's id and the amount spent in GHS. */
public record Purchase(String memberId, BigDecimal amount) {

    private static final int MAX_DECIMAL_PLACES = 2;

    public Purchase {
        if (memberId == null || memberId.isBlank()) {
            throw new InvalidPurchaseException("Member id must not be blank");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPurchaseException("Amount must be more than zero: " + amount);
        }
        if (decimalPlaces(amount) > MAX_DECIMAL_PLACES) {
            throw new InvalidPurchaseException("Amount must have at most "
                    + MAX_DECIMAL_PLACES + " decimal places: " + amount);
        }
    }

    /** Counts real decimal places: trailing zeros do not count, so 10.500 has one. */
    private static int decimalPlaces(BigDecimal amount) {
        return amount.stripTrailingZeros().scale();
    }
}