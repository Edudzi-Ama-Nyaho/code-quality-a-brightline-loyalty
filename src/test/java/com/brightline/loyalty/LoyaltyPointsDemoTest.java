package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs the demo end to end with real classes and checks every line it prints. */
class LoyaltyPointsDemoTest {

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    private LoyaltyPointsDemo demo;

    @BeforeEach
    void setUp() {
        demo = LoyaltyPointsDemo.withSampleMembers(new PrintStream(captured, true));
    }

    @Test
    void carriesOnAfterEachRefusal() {
        demo.runSamplePurchases();

        assertEquals(String.join(System.lineSeparator(),
                "Awarded 10 points to Kofi Mensah (m1) for GHS 100.00",
                "Purchase refused: No member found with id: unknown",
                "Awarded 50 points to Ama Owusu (m2) for GHS 250.00",
                "Purchase refused: Amount must be more than zero: -50.00",
                "Awarded 30 points to Yaa Boateng (m3) for GHS 200.00",
                "Awarded 170 points to Ama Owusu (m2) for GHS 600.00")
                + System.lineSeparator(), captured.toString());
    }

    @Test
    void refusesAmountWithMoreThanTwoDecimalPlaces() {
        demo.awardPoints("m1", "15.555");

        assertEquals("Purchase refused: Amount must have at most 2 decimal places: 15.555"
                + System.lineSeparator(), captured.toString());
    }
}