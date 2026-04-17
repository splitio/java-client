# targeting-engine

A generic, zero-dependency rule evaluation engine extracted from the Split Java SDK. Given a parsed `TargetingRule` and a user key, it deterministically returns a treatment.

## Maven Dependency

```xml
<dependency>
    <groupId>io.split.client</groupId>
    <artifactId>targeting-engine</artifactId>
    <version>4.18.3</version>
</dependency>
```

Requires Java 8+. No runtime dependencies.

## Usage

### 1. Implement `EvaluationContext`

The engine delegates two concerns back to the host application via `EvaluationContext`:

```java
public class MyEvaluationContext implements EvaluationContext {

    // Called for DependencyMatcher and PrerequisitesMatcher
    @Override
    public EvaluationResult evaluate(String matchingKey, String bucketingKey,
                                     String ruleName, Map<String, Object> attributes) {
        return myEngine.getTreatment(matchingKey, bucketingKey, ruleName, attributes);
    }

    // Called for UserDefinedSegmentMatcher
    @Override
    public boolean isInSegment(String segmentName, String key) {
        return mySegmentStorage.isInSegment(segmentName, key);
    }

    // Called for RuleBasedSegmentMatcher
    @Override
    public boolean isInRuleBasedSegment(String segmentName, String key,
                                        String bucketingKey,
                                        Map<String, Object> attributes) {
        return myRuleBasedSegmentEvaluator.isIn(segmentName, key, bucketingKey, attributes);
    }
}
```

### 2. Build a `TargetingRule`

`TargetingRule` contains only the fields needed for evaluation. Metadata (name, configurations, flag sets, etc.) belongs in the calling SDK's domain model.

```java
List<Condition> conditions = List.of(
    new Condition(
        ConditionType.ROLLOUT,
        CombiningMatcher.of(new AllKeysMatcher()),          // match everyone
        List.of(new Partition("on", 50), new Partition("off", 50)),
        "default rule"
    )
);

TargetingRule rule = new TargetingRule(
    123456,   // seed
    false,    // killed
    "off",    // defaultTreatment
    conditions,
    100,      // trafficAllocation (0–100)
    654321,   // trafficAllocationSeed
    2,        // algo: 2 = MurmurHash3 (recommended), 1 = legacy
    null      // prerequisites (List<Prerequisite> or null)
);
```

### 3. Evaluate

`TargetingEngineImpl` is stateless — create once, reuse across threads.

```java
TargetingEngine engine = new TargetingEngineImpl();

EvaluationResult result = engine.evaluate(
    "user-123",             // matchingKey
    null,                   // bucketingKey — null means use matchingKey
    rule,
    Map.of("plan", "pro"),  // attributes — may be null
    context
);

result.treatment   // "on" | "off" | ...
result.label       // EvaluationLabels constant (e.g. "default rule")
```

## Evaluation Labels

| Label constant | When returned |
|---|---|
| `EvaluationLabels.KILLED` | Rule is killed; `defaultTreatment` is returned |
| `EvaluationLabels.PREREQUISITES_NOT_MET` | A prerequisite flag returned the wrong treatment |
| `EvaluationLabels.NOT_IN_SPLIT` | User fell outside traffic allocation |
| `EvaluationLabels.DEFAULT_RULE` | No condition matched; last condition applied |

Additional labels (`DEFINITION_NOT_FOUND`, `EXCEPTION`, `UNSUPPORTED_MATCHER`, `NOT_READY`) are set by the calling SDK, not by the engine.

## Data Model

| Class | Role |
|---|---|
| `TargetingRule` | Full rule definition (seed, conditions, traffic allocation, prerequisites) |
| `Condition` | One targeting condition (`WHITELIST` or `ROLLOUT`) |
| `Partition` | `(treatment, size%)` pair within a condition |
| `Prerequisite` | Dependency on another rule's treatment |
| `CombiningMatcher` | AND-combines a list of `AttributeMatcher`s |
| `AttributeMatcher` | Extracts one attribute then delegates to a `Matcher` |

## Available Matchers

`AllKeysMatcher`, `WhitelistMatcher`, `EqualToMatcher`, `BetweenMatcher`, `BooleanMatcher`,
`ContainsAnyOfMatcher`, `StartsWithAnyOfMatcher`, `EndsWithAnyOfMatcher`, `RegularExpressionMatcher`,
`EqualToSemverMatcher`, `BetweenSemverMatcher`, `GreaterThanOrEqualToSemverMatcher`,
`LessThanOrEqualToSemverMatcher`, `InListSemverMatcher`,
`ContainsAllOfSetMatcher`, `ContainsAnyOfSetMatcher`, `EqualToSetMatcher`, `PartOfSetMatcher`,
`UserDefinedSegmentMatcher`, `RuleBasedSegmentMatcher`, `DependencyMatcher`

## Building

```bash
mvn -pl :targeting-engine clean install
```
