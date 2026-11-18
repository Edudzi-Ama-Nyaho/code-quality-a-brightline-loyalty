package com.brightline.loyalty.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests that the in-memory repository finds the members it was given, and only those. */
class InMemoryMemberRepositoryTest {

    private static final Member KOFI = new Member("m1", "Kofi Mensah", MembershipTier.STANDARD);
    private static final Member AMA = new Member("m2", "Ama Owusu", MembershipTier.GOLD);

    @Test
    @DisplayName("A stored member is found by id")
    void findsStoredMember() {
        InMemoryMemberRepository repository = new InMemoryMemberRepository(List.of(KOFI, AMA));

        assertEquals(Optional.of(AMA), repository.findById("m2"));
    }

    @Test
    @DisplayName("An unknown id finds nothing")
    void findsNothingForUnknownId() {
        InMemoryMemberRepository repository = new InMemoryMemberRepository(List.of(KOFI));

        assertTrue(repository.findById("unknown").isEmpty());
    }

    @Test
    @DisplayName("Two members with the same id are refused")
    void refusesDuplicateIds() {
        Member anotherKofi = new Member("m1", "Kofi Asante", MembershipTier.GOLD);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new InMemoryMemberRepository(List.of(KOFI, anotherKofi)));
        assertEquals("Duplicate member id: m1", exception.getMessage());
    }
}