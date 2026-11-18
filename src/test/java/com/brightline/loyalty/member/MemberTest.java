package com.brightline.loyalty.member;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests that a member can only be created with an id, a name and a tier. */
class MemberTest {

    @Test
    @DisplayName("A member with valid details is accepted and keeps them unchanged")
    void acceptsValidDetails() {
        Member member = new Member("m1", "Kofi Mensah", MembershipTier.STANDARD);

        assertAll(
                () -> assertEquals("m1", member.id()),
                () -> assertEquals("Kofi Mensah", member.name()),
                () -> assertEquals(MembershipTier.STANDARD, member.tier()));
    }

    @ParameterizedTest(name = "A member with id \"{0}\" is refused")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void refusesBlankId(String id) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Member(id, "Kofi Mensah", MembershipTier.STANDARD));
        assertEquals("Member id must not be blank", exception.getMessage());
    }

    @ParameterizedTest(name = "A member with name \"{0}\" is refused")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void refusesBlankName(String name) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Member("m1", name, MembershipTier.STANDARD));
        assertEquals("Member name must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("A member without a tier is refused")
    void refusesMissingTier() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Member("m1", "Kofi Mensah", null));
        assertEquals("Member tier must not be null", exception.getMessage());
    }
}