package com.brightline.loyalty.points;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests that a purchase can only be created with a member id and a valid GHS amount. */
class PurchaseTest {

    @Test
    @DisplayName("A purchase with valid details is accepted and keeps them unchanged")
    void acceptsValidDetails() {
        Purchase purchase = new Purchase("m1", new BigDecimal("15.50"));

        assertAll(
                () -> assertEquals("m1", purchase.memberId()),
                () -> assertEquals(new BigDecimal("15.50"), purchase.amount()));
    }

    @ParameterizedTest(name = "GHS {0} is accepted")
    @ValueSource(strings = {"0.01", "15.5", "10.500"})
    void acceptsAmountsWithAtMostTwoDecimalPlaces(String amount) {
        Purchase purchase = new Purchase("m1", new BigDecimal(amount));

        assertEquals(new BigDecimal(amount), purchase.amount());
    }

    @ParameterizedTest(name = "A purchase for member id \"{0}\" is refused")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void refusesBlankMemberId(String memberId) {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> new Purchase(memberId, new BigDecimal("15.50")));
        assertEquals("Member id must not be blank", exception.getMessage());
    }

    @ParameterizedTest(name = "GHS {0} is refused")
    @ValueSource(strings = {"0", "0.00", "-0.01", "-50"})
    void refusesZeroOrNegativeAmount(String amount) {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> new Purchase("m1", new BigDecimal(amount)));
        assertEquals("Amount must be more than zero: " + amount, exception.getMessage());
    }

    @Test
    @DisplayName("A purchase without an amount is refused")
    void refusesMissingAmount() {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> new Purchase("m1", null));
        assertEquals("Amount must be more than zero: null", exception.getMessage());
    }

    @ParameterizedTest(name = "GHS {0} is refused")
    @ValueSource(strings = {"15.555", "0.001"})
    void refusesMoreThanTwoDecimalPlaces(String amount) {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class,
                () -> new Purchase("m1", new BigDecimal(amount)));
        assertEquals("Amount must have at most 2 decimal places: " + amount,
                exception.getMessage());
    }
}