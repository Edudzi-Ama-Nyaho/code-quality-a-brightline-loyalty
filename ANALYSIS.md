# ANALYSIS: Brightline Loyalty points service

The Analysis section was written and committed before any code change. The Decisions section was added after the refactor.

## Analysis

### Summary

- **Most serious problem:** `PointsCalculator` catches every exception and returns 0 points (finding C1). A member who cannot be found, bad tier data or a bug in the code all end the same way: the customer silently gets 0 points, and nobody can tell that from a real 0 (for example a GHS 5 purchase). The code gives wrong answers today and hides them.
- **Second:** the calculator builds its own member repository (C2), so it cannot be tested with chosen data. This is why C1, C3 and C4 were never caught by a test.
- **The tests:** all 3 AI-generated tests pass, but none of them checks a correct points value. One mocks the class it is meant to test, and one has an assertion that can never fail. Mutation testing confirms it: of 19 deliberate bugs, the tests caught 2 (see section "AI tests that prove nothing").
- **The coming change:** a new membership tier would mean editing the if-chain inside `calculatePoints`, and a tier the code does not know is silently paid at the base rate (C3, C5).

**How to read this document:**
- Findings are labelled C (code problem) and T (AI test that proves nothing); the number is the rank.
- § numbers refer to sections of the **AmaliTech Coding Standards (Java Edition)**, with the section's title in brackets.
- "Ch" refers to a chapter of the SE02 learning path; smell names (Primitive Obsession, Long Method and so on) are from Refactoring.Guru.
- "Where" gives the class and the line numbers in the starter code as delivered.

### Baseline (starter as delivered)

- `mvn clean test`: 3 tests run, 0 failures, BUILD SUCCESS ([test output](evidence/starter-test-output.txt)).
- Demo ([demo output](evidence/starter-demo-output.txt)): m1 STANDARD GHS 100 gives 10; m2 GOLD GHS 250 gives 50; **unknown member GHS 100 gives 0, with no error**.
- JaCoCo (`mvn verify`): 70% of instructions and 50% of branches ran (4 of 8 branches missed); `PointsCalculator` 76% of instructions ([JaCoCo summary](evidence/starter-jacoco-summary.png), [JaCoCo PointsCalculator](evidence/starter-jacoco-pointscalculator.png)).
- **PIT** (`mvn test-compile org.pitest:pitest-maven:mutationCoverage`): **19 mutants, 2 killed (11%); 10 survived, 7 never covered; test strength 17%** ([PIT output](evidence/starter-pit-output.txt), [PIT summary](evidence/starter-pit-summary.png), [PIT PointsCalculator](evidence/starter-pit-pointscalculator.png)).
- The coverage looks respectable, but almost none of it is checked: most bugs PIT put into the code left every test green.
- Built on JDK 25, compiling for release 21.

### Code problems, ranked by business risk

**C1. Every failure is swallowed and turned into 0 points**
- **Where:** `PointsCalculator` lines 10 and 32–35; `InMemoryMemberRepository` line 18
- **Breaks:** §6.1 (Do not ignore caught exceptions)

`findById` returns `null` for an unknown id. Line 17 then calls `member.getTier()`, which throws a `NullPointerException`, and `catch (Exception e)` on line 32 returns 0. The comment on line 33 admits it ("award nothing"). The same catch would also hide any programming error in the method. §6.1 says to catch an exception only where something useful can be done with it, and otherwise let it travel; carrying on as if the operation had succeeded is not acceptable. Evidence: the demo prints `unknown member, GHS 100 -> 0` and the build is green.

**C2. The member repository is hard-wired inside the calculator**
- **Where:** `PointsCalculator` lines 5–7
- **Breaks:** §12.5 (Dependency inversion principle): this is the same pattern as §12.5's BAD example, a field that creates its own implementation
- **Also:** learning path Ch 7, testable-code warning sign (`new` of a collaborator)

Line 7: `private final MemberRepository repository = new InMemoryMemberRepository();`. The comment on lines 5–6 admits that callers and tests cannot pass a different one. The interface `MemberRepository` exists but is not used as a seam, so every test runs against the three hard-coded members, and member storage cannot change without editing the calculator.

**C3. An unknown tier is silently paid at the base rate**
- **Where:** `PointsCalculator` lines 17–23; `Member` line 6
- **Breaks:** §12.2 (Open-closed principle): behaviour is chosen by an if-chain on the tier
- **Smell:** Primitive Obsession (a free-text `String` stands for the tier)

The tier is a free-text `String` compared with `equals` in an if-chain that has no `else`. `"gold"`, `"Gold"` or a new `"PLATINUM"` matches nothing, so the points stay at the base rate with no error. `Member` accepts any tier, including `null`, which then throws on line 17 and is swallowed by C1.

**C4. Purchases and member ids are not validated**
- **Where:** `PointsCalculator` lines 9 and 14
- **Breaks:** no single AmaliTech section covers validation; learning path Ch 4 (validate input and throw a clear exception). Bad input then ends in C1's silent 0 (§6.1)

Nothing checks the amount or the id. An amount of -100 gives `(int) (-100 / 10) = -10` points, and the method prints "Awarded -10 points". A `null` or blank id ends in C1's silent 0.

**C5. Adding a tier means editing existing code**
- **Where:** `PointsCalculator` lines 17–23
- **Breaks:** §12.2 (Open-closed principle): its BAD example has the same note, "every new rule means editing this method"; §8 (Objects and Data Structures): hide internal structure and expose behaviour, not data, and prefer separate behaviour to one method told by a parameter which behaviour to select
- **Smells:** Switch Statements; Feature Envy

Each tier's multiplier lives in the calculator, not with the tier (Feature Envy, §8: `Member` only holds data, and its behaviour lives elsewhere). A new tier means editing the if-chain in the method that every other tier depends on. Replacing it with a `switch` would keep the same smell.

**C6. One method does six jobs**
- **Where:** `PointsCalculator` lines 9–36
- **Breaks:** §12.1 (Single-responsibility principle); §4.1 (Functions should be small: rarely longer than 20 lines); §4.2 (A function should do one thing)
- **Smells:** Long Method; Divergent Change

`calculatePoints` is 28 lines and: (1) looks up the member, (2) works out base points, (3) applies the tier multiplier, (4) adds the big-spender bonus, (5) prints the award, (6) handles failure. These have different reasons to change: marketing changes the bonus, the scheme owner changes the tiers, IT changes member storage, operations changes logging.

**C7. Calculating prints an award**
- **Where:** `PointsCalculator` line 30
- **Breaks:** §4.4 (A function should have no side effects)

Line 30 prints the award and the member's name inside a method named `calculatePoints`. Calculating the points for a quote or a preview would print an award that never happened.

**C8. The points rules are hidden in the code and hard to read**
- **Where:** `PointsCalculator` lines 14, 20 and 26–27
- **Breaks:** §3.4 (Use searchable names: replace magic numbers with named constants): `amount > 500` is the same shape as §3.4's BAD example, `idleMillis > 1_800_000; // what is 1800000?`; §3.1 (Choose names that are descriptive and unambiguous): a comment, "big spenders", explains what a name should; §2.2 (General principles: favour readability)

A reader cannot tell what the scheme's rules are without working through the arithmetic:
- **The bonus:** `if (amount > 500) { points = points + 50; }` under the comment "big spenders". It is not clear whether exactly GHS 500 counts: it does not (GHS 500 gets no bonus, GHS 500.01 does). It is also not clear that the bonus is added after the tier multiplier, so a GOLD member gets +50, not +100. A rule this important is expressed only as two bare numbers and a `>`, so nobody can check it against the scheme.
- **Rounding:** line 14 drops the pennies (GHS 99.99 gives 9 points), and line 20 drops the half point (a SILVER member with 3 base points gets 4, not 4.5). Nothing says whether this is intended.
- **Money as `double`:** the amount in GHS is a `double`, which cannot hold every money value exactly.

**C9. Magic numbers**
- **Where:** `PointsCalculator` lines 14, 18, 20, 22, 26 and 27
- **Breaks:** §3.4 (Use searchable names: replace magic numbers with named constants)

10 (GHS per point), 1, 1.5 and 2 (multipliers), 500 (bonus threshold) and 50 (bonus points) appear as bare numbers, so a rule change means hunting through the code.

**C10. Sample members are hard-coded in a production class**
- **Where:** `InMemoryMemberRepository` lines 10–14
- **Breaks:** §12.1 (Single-responsibility principle): the class looks up members and also decides which members exist

The constructor always loads Kofi, Ama and Yaa. Creating `Member` values with `new` is fine; the problem is test data living in the production repository. This class is really a test fake.

**C11. Comments repeat the code or describe a known flaw**
- **Where:** `PointsCalculator` lines 5–6, 13, 16, 25 and 33; `InMemoryMemberRepository` line 18
- **Breaks:** §10.1 (Only comment things that have business logic complexity)

Lines 13, 16 and 25 restate the code. Lines 5–6, 33 and the repository's line 18 describe a problem instead of fixing it.

### Business effect

C1, C3 and C4 mean the points service **cannot be trusted to give the right answer**:
- a member the system cannot find gets 0 points
- a member on a tier the code does not recognise, including the coming one, is paid at the base rate
- a mistaken negative purchase takes points away

In every case the service reports success. Members find the error in their own balance; the business never sees it in its system. Each wrong balance costs that customer's trust, and repeated across shops it puts Brightline's reputation for a fair loyalty scheme at risk.

Because nothing is reported, these errors cannot be counted or traced. And because of C2, C5 and C6, they are hard to test for and easy to make worse when the new tier is added.

### AI tests that prove nothing, ranked

| # | Test | Lines | Why it proves nothing | Evidence |
|---|---|---|---|---|
| T1 | `calculatesGoldPoints` | 17–23 | Tests a mock, not the code | GOLD never ran; PIT ignores the test |
| T2 | `awardsPointsForAPurchase` | 25–30 | Assertion can never fail | PIT: killed 0 mutants |
| T3 | `awardsPointsToAMember` | 10–15 | Any positive number passes | PIT: killed only "return 0" |

**T1. `calculatesGoldPoints`.** It claims GOLD at GHS 250 gives 50 points. It mocks `PointsCalculator`, the class under test, then asserts the value it told the mock to return. No production line runs, so it only proves that Mockito works. JaCoCo confirms it: the GOLD lines (`PointsCalculator` 21–22) are red, never run by any test, even though a test is named after GOLD. PIT does not even list it under "Tests examined" for `PointsCalculator`, because it runs none of that class's code.

**T2. `awardsPointsForAPurchase`.** It claims a purchase earns points. It uses `assertNotNull` on an `int`: the value is boxed into an `Integer`, which is never null, so the test can never fail. The SILVER value (30) is never checked, and C1's silent 0 would also pass. PIT: it killed none of the 19 mutants. Even "return 0 instead of the points" was not caught by it.

**T3. `awardsPointsToAMember`.** It claims m1 at GHS 100 earns points. It uses `assertTrue(points > 0)`: the right answer is 10, but any positive number passes. Only a 0 would fail it, so a wrong multiplier stays green. PIT: the only mutants in the code it covers that it killed are the two that make the result 0 (`PointsCalculator` line 31 "return 0", and `findById` returning `null`, which C1 turns into 0). Turning `amount / 10` into `amount * 10` (100 times the points) survived.

**What PIT changed in the business rules, and what the tests noticed** ([PIT PointsCalculator](evidence/starter-pit-pointscalculator.png)):

| Line | Bug PIT put in | Business effect if it shipped | Result |
|---|---|---|---|
| 14 | `amount / 10` became `amount * 10` | Every member gets 100 times the points | Survived |
| 17, 19 | Tier checks negated | Members are paid at the wrong tier | Survived |
| 18 | STANDARD `* 1` became `/ 1` | No change (equivalent mutant) | Survived |
| 20 | SILVER `* 1.5` became `/ 1.5` | SILVER members earn less than STANDARD | Survived |
| 21–22 | GOLD check and multiplier changed | Wrong GOLD points | No coverage |
| 26 | `> 500` became `>= 500` | Exactly GHS 500 starts getting the bonus | Survived |
| 26 | Bonus check negated | Bonus paid on small purchases, not big ones | Survived |
| 27 | Bonus `+ 50` became `- 50` | Big spenders lose 50 points | No coverage |
| 30 | `println` removed | No award line printed | Survived |
| 31 | Return 0 instead of the points | Every member gets 0 | **Killed** (only one) |

**Gaps across the suite:**
- No failure tests (unknown member, negative amount, null id). JaCoCo: the `catch` block (`PointsCalculator` 32–34) never ran.
- No boundary tests (GHS 9 and 10, GHS 500 and 501).
- No bonus test; no correct value checked for any tier. JaCoCo: the bonus line (27) never ran, and the bonus `if` (26) was only ever false.
- The tests depend on the three hard-coded members (C2, C10) and print to the console (C7).
- Wildcard static imports (§11.1 Java source files: no wildcard imports).
- §9 (Test-Driven Development) requires every test to make an assertion that would fail if the behaviour were wrong, and says a suite that reaches 100% coverage without such assertions does not meet the standard. None of the three meets that for a points value.

### What the AI got wrong (for the team)

1. **It hid failures instead of reporting them:** a catch-all that returns 0, with a comment that admits it.
2. **It saw its own design flaw and left it:** the comment on the hard-wired repository.
3. **It wrote tests that look like tests:** it mocked the class under test, and wrote assertions that cannot fail.
4. **It tested only the happy path:** no edges, no failures.
5. **It left business rules implicit:** rounding, money as `double`, the 500 boundary.

## Decisions

This section explains the refactor: the design decisions, how the coming tier fits in, what changed in behaviour, and what was kept, fixed or rewritten from the AI's code and tests. Finding numbers (C1 to C11, T1 to T3) refer to the Analysis section above.

### Design decisions

**D1. Tiers are an enum that carries each tier's multiplier.** `MembershipTier` holds STANDARD, SILVER and GOLD, and each knows its own multiplier and how to apply it, so a new tier is one new constant and a tier the code does not know cannot exist. An interface with one class per tier was considered and rejected: as soon as member data comes from text it needs a name-to-class lookup, which brings back the String problem, and it adds files for no gain today. Fixes C3 and C5 (§12.2).

**D2. The member repository is passed into the calculator.** `PointsCalculator` receives a `MemberRepository` through its constructor, so tests choose the members and storage can change without editing the calculator. Fixes C2 (§12.5).

**D3. One job per class.** The old `calculatePoints` is split: `PointsCalculator` works out the points, `PointsAwardPrinter` prints, and `LoyaltyPointsDemo` runs the sample purchases and reports refusals. Calculating no longer prints. The bonus stays a private method of the calculator rather than a class of its own, because it is one line of rule with no reason to change apart from the other rules. Fixes C6 and C7 (§12.1, §4.4).

**D4. An unknown member is a checked exception.** `MemberNotFoundException` extends `Exception`: a mistyped id is a normal event the caller must decide on, and the compiler refuses any caller that ignores it, so the silent 0 cannot come back. Fixes C1 (§6.1).

**D5. An invalid purchase is an unchecked exception.** `InvalidPurchaseException` extends `IllegalArgumentException`: a zero, negative or over-precise amount is the caller's programming error. An amount with more than 2 decimal places is refused, not rounded, because rounding would silently change what the customer spent. Fixes C4.

**D6. Records check their own values.** `Purchase` and `Member` are records whose constructors refuse invalid values, so an invalid purchase or member cannot exist anywhere and every class that uses them is protected. Fixes C3 and C4 (§8, §12.1).

**D7. Money is `BigDecimal`, points are whole and rounded down.** Amounts are built from text so cedis and pesewas are exact; points are always rounded down, as in the starter. Fixes the `double` part of C8.

**D8. The bonus stays "more than GHS 500".** It is now named constants with boundary tests at 499.99, 500.00 and 500.01. Changing it to "GHS 500 or more" was considered and rejected: it would change a business rule nobody asked us to change, so it is kept, made visible and flagged as a question for the business. Fixes C8 and C9 (§3.4).

**D9. Only the demo catches the business exceptions.** `LoyaltyPointsDemo.awardPoints` holds the only try/catch, because the entry point is where something useful can be done: tell the user and carry on with the next purchase. A result object returned instead of an exception was considered and rejected: it would remove the try/catch but add two classes. Fixes C1 (§6.1).

**D10. The calculator returns a `PointsAward`.** `calculateAward` returns the member, amount and points together, so the award line can show the member's name and id without a second lookup, and the printer takes one argument (§4.3). Printing only the member id was considered and rejected: the starter printed the name, and the name is what staff recognise.

**D11. The printer is given where to print.** `PointsAwardPrinter` takes a `PrintStream` through its constructor: the demo passes `System.out` and the tests pass a captured stream, so they can check the exact line (§12.5).

**D12. The repository takes its members from outside.** `InMemoryMemberRepository` stores the members it is given in a `HashMap` keyed by id and refuses duplicate ids, so no sample data lives in production code. Fixes C10.

The comments that repeated the code or described a known flaw are gone (C11, §10.1). Each class has a one-line Javadoc saying its job; a method has one only where the code cannot show the reason.

### How the new tier slots in

Adding a tier, for example PLATINUM at three times the base points, means:

1. **One new constant** in `MembershipTier`: `PLATINUM(new BigDecimal("3"))`. No method, no `if` and no other class changes.
2. **New rows** in `src/test/resources/points-rules.csv`, such as `PLATINUM,10.00,3,Platinum triples base points` and `PLATINUM,500.01,200,Bonus is added after the multiplier`.

The build enforces step 2: `coversEveryMembershipTier` fails until the new tier has rows in the rules table. If the new tier needs a different kind of rule (not just a multiplier), its constant can override `applyMultiplier` without touching the others.

In the starter, the same change meant editing the if-chain inside `calculatePoints`, the method every tier depends on, and a tier the code did not know was silently paid at the base rate.

### Behaviour changes

Every points value for a valid purchase is unchanged: m1 still earns 10 points on GHS 100 and m2 50 points on GHS 250 ([after demo output](evidence/after-demo-output.txt)), and the rules table checks the boundaries. What changed:

| Input | Starter | Now |
|---|---|---|
| Unknown member | 0 points, no error | Refused: "No member found with id: unknown" |
| Zero or negative amount | Negative points | Refused: "Amount must be more than zero" |
| Blank or missing member id | 0 points, no error | Refused: "Member id must not be blank" |
| More than 2 decimal places | Accepted as `double` | Refused, not rounded |
| Unknown tier name | Paid at the base rate | Cannot exist: the tier is an enum |
| Two members with one id | Second replaces the first | Refused: "Duplicate member id" |
| Award line | "Awarded 10 points to Kofi Mensah" | "Awarded 10 points to Kofi Mensah (m1) for GHS 100.00" |
| A refusal in the demo | Hidden as 0 | Printed, and the next purchase still runs |

### Points and money

- **Type:** amounts are `BigDecimal` in GHS with at most 2 decimal places; points are `int`.
- **Rounding:** always down, in two places only. `PointsCalculator.basePoints` gives 1 point per full GHS 10 (GHS 19.99 earns 1). `MembershipTier.applyMultiplier` drops part points (SILVER on 3 base points earns 4, not 4.5). Both match the starter.
- **Comparisons** use `compareTo`, not `equals`: `BigDecimal.equals` treats 500 and 500.00 as different.

### Kept, fixed or rewritten

| AI item | Decision | Evidence |
|---|---|---|
| `PointsCalculator` | Rewritten | C1, C2, C6 to C9; the rules themselves kept, proven by the rules table |
| `Member` | Rewritten as a record | String tier (C3); no validation (C4) |
| `MemberRepository` | Kept, return type fixed | A sound seam; now returns `Optional` instead of `null` (C1) |
| `InMemoryMemberRepository` | Fixed | Members passed in (C10); `Optional`; duplicates refused |
| `Demo` | Rewritten as `LoyaltyPointsDemo` | Name said nothing (§3.1); now reports refusals |
| T1 `calculatesGoldPoints` | Deleted, replaced | Mocked the class under test; `PointsCalculatorTest` mocks only the repository |
| T2 `awardsPointsForAPurchase` | Deleted, replaced | Could never fail; SILVER now checked for exact values |
| T3 `awardsPointsToAMember` | Deleted, replaced | Any positive number passed; STANDARD now checked for exactly 10 |

### Tests

75 tests, all passing on `mvn clean verify`:

| Test class | Tests | Checks |
|---|---|---|
| `MembershipTierTest` | 6 | Each multiplier, rounding down, negative refused |
| `MemberTest` | 8 | Valid member; blank id or name and missing tier refused |
| `InMemoryMemberRepositoryTest` | 3 | Finds, finds nothing, refuses duplicates |
| `PurchaseTest` | 14 | Amount and id boundaries |
| `PointsCalculatorTest` | 15 | Mocked repository; every tier, rounding and bonus boundary, refusals |
| `PointsAwardPrinterTest` | 9 | Exact printed lines |
| `LoyaltyPointsDemoTest` | 2 | Whole chain with real classes; carries on after a refusal |
| `PointsRulesTableTest` | 18 | 17 business examples, and every tier has rows |

Every expected value was worked out from the business rules first, then checked against the code, never copied from it. Every mocked test asserts a result or an exception, not only that a mock was called.

**Which test catches a planted bug in each rule:**

| Rule | Bug | Test that fails |
|---|---|---|
| 1 point per full GHS 10 | Rounding up, or dividing wrongly | `roundsBasePointsDown` (19.99 and 9.99 rows) |
| SILVER times 1.5, rounded down | Rounding half up | `MembershipTierTest` SILVER 3 row; rules table "Silver 4.5 is rounded down" |
| GOLD times 2 | Wrong multiplier | `appliesTierMultiplier` GOLD row |
| Bonus only above GHS 500 | `>` changed to `>=` | `addsBonusOnlyAboveFiveHundred` 500.00 row; three rules-table rows |
| Bonus added after the multiplier | Bonus multiplied too | Rules table "Bonus is not multiplied" (GOLD 600) |
| Unknown member refused | Returns 0 again | `refusesUnknownMember` |
| Invalid amount refused | Check removed | `refusesZeroOrNegativeAmount` |

The bonus bug was planted for real: the build failed on four tests ([planted bug evidence](evidence/gate-catches-planted-bug.txt)).

**Coverage and mutation testing after the refactor:**
- PIT on the `member` and `points` packages: **13 mutants, 13 killed (100%)**, line coverage 35 of 35. The starter's tests killed 2 of 19 ([PIT after](evidence/after-pit-summary.png)).
- JaCoCo over the whole project: **97% of instructions** (13 of 436 missed) and **100% of branches** (0 of 30 missed); the `member` and `points` packages are 100% on both ([JaCoCo after](evidence/after-jacoco-summary.png)). The 13 missed instructions are all in `LoyaltyPointsDemo.main`, the last-resort `catch` ([JaCoCo demo](evidence/after-jacoco-demo-main.png)): reaching it means breaking the program on purpose, and its `System.exit` would stop the test run. Everything `main` calls is covered by `LoyaltyPointsDemoTest`.

### Improvement: the build fails if the rules stop being really tested

Brightline's coming change is a new tier. In the starter, a tier could be added, every test would stay green, and nobody would have checked its points. The improvement makes that impossible, in three layers:

1. **A business rules table**, `src/test/resources/points-rules.csv`: tier, GHS amount, expected points and the rule in words. The business can read and check it without reading Java; the GHS 500 question from C8 is visible as "Exactly GHS 500 earns no bonus". `PointsRulesTableTest` runs every row through the real repository and calculator, and a failure prints the rule that broke.
2. **A tier guard**, `coversEveryMembershipTier`: the build fails if any tier in the enum has no rows in the table, so the new tier cannot ship without its rules written down and checked.
3. **A mutation gate** in `pom.xml`: `mvn verify` runs PIT on the `member` and `points` packages with a 100% threshold, so if any planted bug in the rules survives, the build fails. A suite like the starter's, which caught 2 of 19, could not pass.

### Tooling changes

- **JaCoCo 0.8.12 to 0.8.14:** 0.8.12 cannot read JDK 25 classes.
- **PIT 1.16.1 to 1.22.1, JUnit 5 plugin 1.2.1 to 1.2.3:** 1.16.1 could not read JDK 25 classes and reported covered code as uncovered.
- **Every Maven plugin pinned** to a version, so the build is the same on every machine.
- **Mockito warning (not fixed):** on JDK 25 Mockito attaches itself at run time and prints a warning. It is harmless today; a later JDK will block it. The fix is to load Mockito as a Java agent in the Surefire and PIT settings.

### Personal AI evaluation

I used an AI assistant (Claude) throughout this lab. This is how the work was split.

**Analysis.** I ran the starter, its tests, JaCoCo and PIT myself and wrote my own notes on what was wrong. Claude helped with the editing and wording that turned my notes into the Analysis section.

**High-level design and decisions.** I worked out the design first: three packages (`member`, `points` and the root), what each class is responsible for, and where exceptions are caught.

**Code and tests.** Claude wrote most of the code and tests, one class at a time. I reviewed and evaluated each method and test it wrote. The review led to these changes:
- "1 points" fixed to "1 point"
- a GHS 499.99 row added, because there was no "just below" case for the bonus
- `awardPoints` builds the `Purchase` inside its `try`, because the `Purchase` constructor is what refuses a bad amount
- PIT upgraded after it was found unreliable on JDK 25