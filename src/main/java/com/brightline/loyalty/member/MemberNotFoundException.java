package com.brightline.loyalty.member;

import java.io.Serial;

/** Thrown when no member has the given id: a refusal the caller must handle. */
public class MemberNotFoundException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public MemberNotFoundException(String memberId) {
        super("No member found with id: " + memberId);
    }
}