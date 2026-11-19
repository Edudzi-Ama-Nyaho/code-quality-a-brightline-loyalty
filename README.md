# Brightline Loyalty: points service

## What it is and why it matters

Brightline Loyalty gives its members points when they buy something. This service works out how many points a purchase earns:

- **1 base point for every full GHS 10 spent**, rounded down (GHS 19.99 earns 1)
- **multiplied by the member's tier:** STANDARD x1, SILVER x1.5, GOLD x2, rounded down to whole points
- **plus 50 bonus points** when the purchase is more than GHS 500 (exactly GHS 500 earns no bonus)

The first version was written quickly with an AI assistant. It looked finished, but it silently gave 0 points when something went wrong, and its tests did not check any real points value. This version refuses bad input with a clear message, is tested against every rule, and is ready for the new membership tier Brightline plans to add. The full review is in [ANALYSIS.md](ANALYSIS.md).

## How it works

Each purchase passes through these classes in order:

```
 LoyaltyPointsDemo        runs each sample purchase
        │
        ▼
 Purchase                 checks the member id and the GHS amount
        │
        ▼
 PointsCalculator         finds the member, applies the tier and the bonus
        │
        ▼
 PointsAward              holds the member, the amount and the points
        │
        ▼
 PointsAwardPrinter       prints "Awarded 10 points to Kofi Mensah (m1) for GHS 100.00"
```

If the purchase is invalid or the member is unknown, the refusal goes back to `LoyaltyPointsDemo`, which prints "Purchase refused:" with the reason and moves on to the next purchase.

## What you need

| Tool | Check with | Notes |
|---|---|---|
| Java (JDK) 21 or newer | `java -version` | Built on JDK 25, compiled for Java 21 |
| Maven | `mvn -v` | The build tool that compiles and tests the code; IntelliJ IDEA includes its own |

If `mvn` is not found in a terminal, use IntelliJ instead: open the **Maven** panel, click the **m** icon (Execute Maven Goal) and type the command from the steps below.

## How to run it

1. Open a terminal in the project folder.
2. Run:
   ```
   mvn compile exec:java -Dexec.mainClass=com.brightline.loyalty.LoyaltyPointsDemo
   ```
3. The demo runs six sample purchases, including one for a member who does not exist and one with a negative amount. You should see:
   ```
   Awarded 10 points to Kofi Mensah (m1) for GHS 100.00
   Purchase refused: No member found with id: unknown
   Awarded 50 points to Ama Owusu (m2) for GHS 250.00
   Purchase refused: Amount must be more than zero: -50.00
   Awarded 30 points to Yaa Boateng (m3) for GHS 200.00
   Awarded 170 points to Ama Owusu (m2) for GHS 600.00
   ```
   A refused purchase is reported and the next one still runs.

## How to run the tests

**Quick check** (the tests only):
```
mvn clean test
```
Passing looks like `Tests run: 76, Failures: 0, Errors: 0, Skipped: 0` and then `BUILD SUCCESS`.

**Full check** (tests, coverage and the mutation gate):
```
mvn clean verify
```
This also:
- measures **coverage** with JaCoCo: which lines the tests run. Report: `target/site/jacoco/index.html`
- runs **mutation testing** with PIT: it plants small bugs in the points rules and checks that a test catches each one. Report: `target/pit-reports/index.html`

The build **fails** if any test fails, or if any planted bug in the rules goes uncaught. Passing ends with `Generated 14 mutations Killed 14 (100%)` and `BUILD SUCCESS`.

A few lines starting `Mockito is currently self-attaching` and `WARNING: A Java agent has been loaded dynamically` are expected on JDK 25 and do not affect the result (see "Known limits").

## Files

```
src/main/java/com/brightline/loyalty/
├── LoyaltyPointsDemo.java          Runs the sample purchases; reports refusals
├── PointsAwardPrinter.java         Prints each award or refusal
├── member/
│   ├── Member.java                 A member: id, name, tier
│   ├── MembershipTier.java         The tiers and their multipliers
│   ├── MemberRepository.java       Finds members by id
│   ├── InMemoryMemberRepository.java   Keeps members in memory
│   └── MemberNotFoundException.java    No member has that id
└── points/
    ├── Purchase.java               A purchase: member id and GHS amount
    ├── PointsCalculator.java       Works out the points
    ├── PointsAward.java            The result: member, amount, points
    └── InvalidPurchaseException.java   The purchase details are invalid

src/test/java/...                   Tests for every class with behaviour, plus
                                    LoyaltyPointsDemoTest and PointsRulesTableTest
src/test/resources/points-rules.csv The business rules as examples
evidence/                           Test, demo, coverage and mutation results, before and after
ANALYSIS.md                         What was wrong, and the decisions behind the fix
```

## How to add a membership tier

For example, PLATINUM at three times the base points:

1. In `MembershipTier.java`, add one constant after GOLD (change the `;` after GOLD to `,`):
   ```java
   PLATINUM(new BigDecimal("3"));
   ```
2. In `src/test/resources/points-rules.csv`, add rows that show the new tier's points, for example:
   ```
   PLATINUM,10.00,3,Platinum triples base points
   PLATINUM,500.01,200,Bonus is added after the multiplier
   ```
3. Run `mvn clean verify`.

No other file changes. Step 2 is required: the build fails until the new tier has rows in the rules table, so a tier cannot go live without its points being checked.

## Design decisions and known limits

The main decisions, with the reasons and alternatives, are in the Decisions section of [ANALYSIS.md](ANALYSIS.md). In short:

- each class has one job, and is given the other classes it needs instead of creating them itself
- each tier carries its own multiplier, so a new tier is one new constant
- invalid input is refused with a clear message instead of turning into 0 points
- money is exact (`BigDecimal`), and points are always rounded down

**Known limits:**
- Members are kept in memory only; the three sample members live in the demo.
- Each purchase is worked out on its own; no points balance is stored.
- The bonus starts above GHS 500, as in the original. Whether exactly GHS 500 should earn it is a question for the business.
- The last-resort error handler in `LoyaltyPointsDemo.main` is not covered by a test, because reaching it means breaking the program on purpose.
- On JDK 25, Mockito prints a warning during the tests. It is harmless today; a later JDK will block it unless Mockito is loaded as a Java agent.

**A note on Maven:** if a terminal says `mvn` is not recognised, Maven is not on your computer's PATH (the list of folders Windows searches for commands). Either run the commands from IntelliJ's Maven panel as described in "What you need", or install Maven from maven.apache.org and add its `bin` folder to PATH.