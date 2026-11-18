package com.brightline.loyalty;

import com.brightline.loyalty.member.Member;
import com.brightline.loyalty.points.PointsAward;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Objects;

/** Prints the outcome of each purchase: the points awarded or why it was refused. */
public final class PointsAwardPrinter {

    private static final int GHS_DECIMAL_PLACES = 2;

    private final PrintStream output;

    public PointsAwardPrinter(PrintStream output) {
        this.output = Objects.requireNonNull(output, "Output stream must not be null");
    }

    public void printAward(PointsAward award) {
        Member member = award.member();
        output.println("Awarded " + pointsText(award.points()) + " to " + member.name()
                + " (" + member.id() + ") for GHS " + formatGhs(award.amount()));
    }

    public void printRefusal(String reason) {
        output.println("Purchase refused: " + reason);
    }

    private String pointsText(int points) {
        return points + (points == 1 ? " point" : " points");
    }

    private String formatGhs(BigDecimal amount) {
        return amount.setScale(GHS_DECIMAL_PLACES).toPlainString();
    }
}