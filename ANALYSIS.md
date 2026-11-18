# ANALYSIS: Brightline Loyalty points service

The Analysis section was written and committed before any code change. The Decisions section is added after the refactor.

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

To be added after the refactor.