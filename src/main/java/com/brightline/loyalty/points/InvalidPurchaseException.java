package com.brightline.loyalty.points;

import java.io.Serial;

/** Thrown when a purchase's details are invalid: a programming error by the caller. */
public class InvalidPurchaseException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidPurchaseException(String message) {
        super(message);
    }
}