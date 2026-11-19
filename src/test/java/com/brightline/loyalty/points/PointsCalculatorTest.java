package com.brightline.loyalty.points;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brightline.loyalty.member.Member;
import com.brightline.loyalty.member.MemberNotFoundException;
import com.brightline.loyalty.member.MemberRepository;
import com.brightline.loyalty.member.MembershipTier;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests the points a member earns on a purchase. */
@ExtendWith(MockitoExtension.class)
class PointsCalculatorTest {

    private static final String MEMBER_ID = "m1";

    @Mock
    private MemberRepository memberRepository;

    private PointsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PointsCalculator(memberRepository);
    }

    @ParameterizedTest(name = "{0} spending GHS {1} earns {2}")
    @CsvSource({
            "STANDARD, 100.00, 10",
            "SILVER,   200.00, 30",
            "GOLD,     250.00, 50",
            "SILVER,    30.00,  4"
    })
    void appliesTierMultiplier(MembershipTier tier, BigDecimal amount, int expected)
            throws MemberNotFoundException {
        givenMemberOnTier(tier);
        assertEquals(expected, pointsFor(amount));
    }

    @ParameterizedTest(name = "GHS {0} earns {1} base points")
    @CsvSource({"9.99, 0", "10.00, 1", "15.50, 1", "19.99, 1", "20.00, 2"})
    void roundsBasePointsDown(BigDecimal amount, int expected) throws MemberNotFoundException {
        givenMemberOnTier(MembershipTier.STANDARD);
        assertEquals(expected, pointsFor(amount));
    }

    @ParameterizedTest(name = "{0} spending GHS {1} earns {2}")
    @CsvSource({
            "STANDARD, 500.00,  50",
            "STANDARD, 500.01, 100",
            "GOLD,     600.00, 170"
    })
    void addsBonusOnlyAboveFiveHundred(MembershipTier tier, BigDecimal amount, int expected)
            throws MemberNotFoundException {
        givenMemberOnTier(tier);
        assertEquals(expected, pointsFor(amount));
    }

    @Test
    void refusesUnknownMember() {
        when(memberRepository.findById("unknown")).thenReturn(Optional.empty());
        Purchase purchase = new Purchase("unknown", new BigDecimal("10.00"));

        MemberNotFoundException exception = assertThrows(MemberNotFoundException.class,
                () -> calculator.calculateAward(purchase));
        assertEquals("No member found with id: unknown", exception.getMessage());
        verify(memberRepository).findById("unknown");
    }

    @Test
    void refusesMissingRepository() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new PointsCalculator(null));
        assertEquals("Member repository must not be null", exception.getMessage());
    }

    @Test
    void awardCarriesMemberAndAmount() throws MemberNotFoundException {
        Member member = new Member(MEMBER_ID, "Kofi Mensah", MembershipTier.STANDARD);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        BigDecimal amount = new BigDecimal("100.00");

        PointsAward award = calculator.calculateAward(new Purchase(MEMBER_ID, amount));

        assertAll(
                () -> assertEquals(member, award.member()),
                () -> assertEquals(amount, award.amount()),
                () -> assertEquals(10, award.points()));
    }

    private void givenMemberOnTier(MembershipTier tier) {
        when(memberRepository.findById(MEMBER_ID))
                .thenReturn(Optional.of(new Member(MEMBER_ID, "Test Member", tier)));
    }

    private int pointsFor(BigDecimal amount) throws MemberNotFoundException {
        return calculator.calculateAward(new Purchase(MEMBER_ID, amount)).points();
    }
}