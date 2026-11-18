package com.brightline.loyalty.member;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A membership tier; each tier multiplies a purchase's base points by its own rate. */
public enum MembershipTier {
    STANDARD(new BigDecimal("1")),
    SILVER(new BigDecimal("1.5")),
    GOLD(new BigDecimal("2"));

    private final BigDecimal multiplier;

    MembershipTier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    /** Multiplies base points by this tier's rate, rounded down to whole points. */
    public int applyMultiplier(int basePoints) {
        if (basePoints < 0) {
            throw new IllegalArgumentException("Base points must not be negative: " + basePoints);
        }
        return BigDecimal.valueOf(basePoints)
                .multiply(multiplier)
                .setScale(0, RoundingMode.DOWN)
                .intValueExact();
    }
}