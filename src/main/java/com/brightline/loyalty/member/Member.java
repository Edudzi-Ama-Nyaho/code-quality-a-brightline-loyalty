package com.brightline.loyalty.member;

/** A loyalty scheme member: an id, a name and the tier that sets their points rule. */
public record Member(String id, String name, MembershipTier tier) {

    public Member {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Member id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Member name must not be blank");
        }
        if (tier == null) {
            throw new IllegalArgumentException("Member tier must not be null");
        }
    }
}