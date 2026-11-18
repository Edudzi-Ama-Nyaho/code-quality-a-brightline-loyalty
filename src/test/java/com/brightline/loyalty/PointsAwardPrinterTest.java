package com.brightline.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.brightline.loyalty.member.Member;
import com.brightline.loyalty.member.MembershipTier;
import com.brightline.loyalty.points.PointsAward;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests the lines printed for awarded and refused purchases. */
class PointsAwardPrinterTest {

    private static final Member KOFI = new Member("m1", "Kofi Mensah", MembershipTier.STANDARD);

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    private PointsAwardPrinter printer;

    @BeforeEach
    void setUp() {
        printer = new PointsAwardPrinter(new PrintStream(captured, true));
    }

    @ParameterizedTest(name = "GHS {0} prints as GHS {1}")
    @CsvSource({"100, 100.00", "15.5, 15.50", "10.500, 10.50", "0.01, 0.01"})
    void printsAmountToTwoDecimalPlaces(BigDecimal amount, String shown) {
        printer.printAward(new PointsAward(KOFI, amount, 10));

        assertEquals("Awarded 10 points to Kofi Mensah (m1) for GHS " + shown
                + System.lineSeparator(), captured.toString());
    }

    @ParameterizedTest(name = "{0} prints as {1}")
    @CsvSource({"0, 0 points", "1, 1 point", "2, 2 points"})
    void printsPointInSingularOnlyForOne(int points, String shown) {
        printer.printAward(new PointsAward(KOFI, new BigDecimal("100"), points));

        assertEquals("Awarded " + shown + " to Kofi Mensah (m1) for GHS 100.00"
                + System.lineSeparator(), captured.toString());
    }

    @Test
    void printsRefusalReason() {
        printer.printRefusal("No member found with id: unknown");

        assertEquals("Purchase refused: No member found with id: unknown"
                + System.lineSeparator(), captured.toString());
    }

    @Test
    void refusesMissingOutputStream() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new PointsAwardPrinter(null));
        assertEquals("Output stream must not be null", exception.getMessage());
    }
}