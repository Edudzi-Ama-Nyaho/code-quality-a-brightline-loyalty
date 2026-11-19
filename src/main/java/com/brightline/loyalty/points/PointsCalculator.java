package com.brightline.loyalty.points;

import com.brightline.loyalty.member.Member;
import com.brightline.loyalty.member.MemberNotFoundException;
import com.brightline.loyalty.member.MemberRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Works out the points a member earns on a purchase. */
public final class PointsCalculator {

    private static final BigDecimal GHS_PER_POINT = new BigDecimal("10");
    private static final BigDecimal BONUS_THRESHOLD_GHS = new BigDecimal("500");
    private static final int BONUS_POINTS = 50;

    private final MemberRepository memberRepository;

    public PointsCalculator(MemberRepository memberRepository) {
        this.memberRepository = Objects.requireNonNull(memberRepository,
                "Member repository must not be null");
    }

    public PointsAward calculateAward(Purchase purchase) throws MemberNotFoundException {
        Member member = memberRepository.findById(purchase.memberId())
                .orElseThrow(() -> new MemberNotFoundException(purchase.memberId()));
        int tierPoints = member.tier().applyMultiplier(basePoints(purchase.amount()));
        int points = addBonusPoints(purchase.amount(), tierPoints);
        return new PointsAward(member, purchase.amount(), points);
    }

    private int basePoints(BigDecimal amount) {
        return amount.divide(GHS_PER_POINT, 0, RoundingMode.DOWN).intValueExact();
    }

    /** Adds the flat bonus for spending more than GHS 500; exactly GHS 500 earns none. */
    private int addBonusPoints(BigDecimal amount, int tierPoints) {
        if (amount.compareTo(BONUS_THRESHOLD_GHS) > 0) {
            return tierPoints + BONUS_POINTS;
        }
        return tierPoints;
    }
}