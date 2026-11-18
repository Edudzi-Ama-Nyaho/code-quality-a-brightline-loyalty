package com.brightline.loyalty.points;

import com.brightline.loyalty.member.Member;
import java.math.BigDecimal;

/** The points a member earned on one purchase, with what is needed to report it. */
public record PointsAward(Member member, BigDecimal amount, int points) {
}