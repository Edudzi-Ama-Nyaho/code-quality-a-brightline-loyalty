package com.brightline.loyalty.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests each membership tier's points rule. */
class MembershipTierTest {

    @ParameterizedTest(name = "{0} with {1} base points earns {2}")
    @CsvSource({
            "STANDARD, 10, 10",
            "SILVER,   10, 15",
            "SILVER,    3,  4",
            "GOLD,     10, 20",
            "GOLD,      0,  0"
    })
    @DisplayName("Each tier applies its own multiplier, rounding down")
    void appliesTierMultiplier(MembershipTier tier, int basePoints, int expectedPoints) {
        assertEquals(expectedPoints, tier.applyMultiplier(basePoints));
    }

    @Test
    @DisplayName("Negative base points are refused with a clear message")
    void refusesNegativeBasePoints() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> MembershipTier.GOLD.applyMultiplier(-1));
        assertEquals("Base points must not be negative: -1", exception.getMessage());
    }
}