package com.brightline.loyalty.points;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.brightline.loyalty.member.InMemoryMemberRepository;
import com.brightline.loyalty.member.Member;
import com.brightline.loyalty.member.MemberNotFoundException;
import com.brightline.loyalty.member.MembershipTier;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/** Checks the business rules table against the real classes, and that it covers every tier. */
class PointsRulesTableTest {

    private static final String RULES_TABLE = "/points-rules.csv";

    @ParameterizedTest(name = "{3}: {0} spending GHS {1} earns {2}")
    @CsvFileSource(resources = RULES_TABLE, numLinesToSkip = 1)
    void followsTheRulesTable(MembershipTier tier, BigDecimal amount, int expectedPoints,
                              String rule) throws MemberNotFoundException {
        Member member = new Member("m1", "Rules Member", tier);
        PointsCalculator calculator =
                new PointsCalculator(new InMemoryMemberRepository(List.of(member)));

        PointsAward award = calculator.calculateAward(new Purchase("m1", amount));

        assertEquals(expectedPoints, award.points(), rule);
    }

    @Test
    void coversEveryMembershipTier() throws IOException {
        assertEquals(EnumSet.allOf(MembershipTier.class), tiersInRulesTable(),
                "Every membership tier needs rows in " + RULES_TABLE);
    }

    private Set<MembershipTier> tiersInRulesTable() throws IOException {
        InputStream table = Objects.requireNonNull(
                getClass().getResourceAsStream(RULES_TABLE), "Missing " + RULES_TABLE);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(table, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(line -> MembershipTier.valueOf(line.split(",")[0].trim()))
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(MembershipTier.class)));
        }
    }
}