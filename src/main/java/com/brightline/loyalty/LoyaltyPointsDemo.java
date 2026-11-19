package com.brightline.loyalty;

import com.brightline.loyalty.member.InMemoryMemberRepository;
import com.brightline.loyalty.member.Member;
import com.brightline.loyalty.member.MemberNotFoundException;
import com.brightline.loyalty.member.MembershipTier;
import com.brightline.loyalty.points.InvalidPurchaseException;
import com.brightline.loyalty.points.PointsCalculator;
import com.brightline.loyalty.points.Purchase;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;

/** Runs the points service on sample purchases and prints the outcome of each. */
public final class LoyaltyPointsDemo {

    private static final List<Member> SAMPLE_MEMBERS = List.of(
            new Member("m1", "Kofi Mensah", MembershipTier.STANDARD),
            new Member("m2", "Ama Owusu", MembershipTier.GOLD),
            new Member("m3", "Yaa Boateng", MembershipTier.SILVER));

    private final PointsCalculator calculator;
    private final PointsAwardPrinter printer;

    private LoyaltyPointsDemo(PointsCalculator calculator, PointsAwardPrinter printer) {
        this.calculator = calculator;
        this.printer = printer;
    }

    public static void main(String[] args) {
        try {
            withSampleMembers(System.out).runSamplePurchases();
        } catch (RuntimeException e) {
            System.err.println("Loyalty points demo stopped: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static LoyaltyPointsDemo withSampleMembers(PrintStream output) {
        PointsCalculator calculator =
                new PointsCalculator(new InMemoryMemberRepository(SAMPLE_MEMBERS));
        return new LoyaltyPointsDemo(calculator, new PointsAwardPrinter(output));
    }

    void runSamplePurchases() {
        awardPoints("m1", "100.00");
        awardPoints("unknown", "100.00");
        awardPoints("m2", "250.00");
        awardPoints("m1", "-50.00");
        awardPoints("m3", "200.00");
        awardPoints("m2", "600.00");
    }

    void awardPoints(String memberId, String amountGhs) {
        try {
            Purchase purchase = new Purchase(memberId, parseAmount(amountGhs));
            printer.printAward(calculator.calculateAward(purchase));
        } catch (MemberNotFoundException | InvalidPurchaseException e) {
            printer.printRefusal(e.getMessage());
        }
    }

    private static BigDecimal parseAmount(String amountGhs) {
        try {
            return new BigDecimal(amountGhs);
        } catch (NumberFormatException e) {
            throw new InvalidPurchaseException("Amount is not a number: " + amountGhs, e);
        }
    }
}