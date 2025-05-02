package io.split.engine.matchers;

import com.google.common.collect.Lists;
import io.split.client.dtos.ConditionType;
import io.split.client.dtos.MatcherCombiner;
import io.split.client.dtos.SplitChange;
import io.split.client.utils.Json;
import io.split.client.utils.RuleBasedSegmentsToUpdate;
import io.split.engine.evaluator.EvaluationContext;
import io.split.engine.evaluator.Evaluator;
import io.split.engine.experiments.ParsedCondition;
import io.split.engine.experiments.ParsedRuleBasedSegment;
import io.split.engine.experiments.RuleBasedSegmentParser;
import io.split.engine.matchers.strings.WhitelistMatcher;
import io.split.storages.RuleBasedSegmentCache;
import io.split.storages.SegmentCache;
import io.split.storages.memory.RuleBasedSegmentCacheInMemoryImp;
import io.split.storages.memory.SegmentCacheInMemoryImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static io.split.client.utils.RuleBasedSegmentProcessor.processRuleBasedSegmentChanges;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RuleBasedSegmentMatcherTest {
    @Test
    public void works() {
        Evaluator evaluator = Mockito.mock(Evaluator.class);
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        RuleBasedSegmentCache ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        EvaluationContext evaluationContext = new EvaluationContext(evaluator, segmentCache, ruleBasedSegmentCache);
        AttributeMatcher whiteListMatcher = AttributeMatcher.vanilla(new WhitelistMatcher(Lists.newArrayList("test_1", "admin")));
        CombiningMatcher whitelistCombiningMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(whiteListMatcher));

        AttributeMatcher ruleBasedSegmentMatcher = AttributeMatcher.vanilla(new RuleBasedSegmentMatcher("sample_rule_based_segment"));
        CombiningMatcher ruleBasedSegmentCombinerMatcher = new CombiningMatcher(MatcherCombiner.AND, Lists.newArrayList(ruleBasedSegmentMatcher));
        ParsedCondition ruleBasedSegmentCondition = new ParsedCondition(ConditionType.ROLLOUT, ruleBasedSegmentCombinerMatcher, null, "test rbs rule");
        ParsedRuleBasedSegment parsedRuleBasedSegment = new ParsedRuleBasedSegment("sample_rule_based_segment",
                Lists.newArrayList(new ParsedCondition(ConditionType.WHITELIST, whitelistCombiningMatcher, null, "whitelist label")),"user",
                123, Lists.newArrayList("mauro@test.io","gaston@test.io"), Lists.newArrayList());
        ruleBasedSegmentCache.update(Lists.newArrayList(parsedRuleBasedSegment), null, 123);

        RuleBasedSegmentMatcher matcher = new RuleBasedSegmentMatcher("sample_rule_based_segment");

        assertThat(matcher.match("mauro@test.io", null, null, evaluationContext), is(false));
        assertThat(matcher.match("admin", null, null, evaluationContext), is(true));

        assertThat(matcher.match("foo", null, null, evaluationContext), is(false));
        assertThat(matcher.match(null, null, null, evaluationContext), is(false));
    }

    @Test
    public void usingRbsInConditionTest() throws IOException {
        String load = new String(Files.readAllBytes(Paths.get("src/test/resources/rule_base_segments.json")), StandardCharsets.UTF_8);
        Evaluator evaluator = Mockito.mock(Evaluator.class);
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        RuleBasedSegmentCache ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        EvaluationContext evaluationContext = new EvaluationContext(evaluator, segmentCache, ruleBasedSegmentCache);

        SplitChange change = Json.fromJson(load, SplitChange.class);
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        RuleBasedSegmentsToUpdate ruleBasedSegmentsToUpdate = processRuleBasedSegmentChanges(ruleBasedSegmentParser,
                change.ruleBasedSegments.d);
        ruleBasedSegmentCache.update(ruleBasedSegmentsToUpdate.getToAdd(), null, 123);
        RuleBasedSegmentMatcher matcher = new RuleBasedSegmentMatcher("dependent_rbs");
        HashMap<String, Object> attrib1 =  new HashMap<String, Object>() {{
            put("email", "mauro@@split.io");
        }};
        HashMap<String, Object> attrib2 =  new HashMap<String, Object>() {{
            put("email", "bilal@@split.io");
        }};
        assertThat(matcher.match("mauro@split.io", null, attrib1, evaluationContext), is(false));
        assertThat(matcher.match("bilal@split.io", null, attrib2, evaluationContext), is(true));
    }

    @Test
    public void usingSegmentInExcludedTest() throws IOException {
        String load = new String(Files.readAllBytes(Paths.get("src/test/resources/rule_base_segments3.json")), StandardCharsets.UTF_8);
        Evaluator evaluator = Mockito.mock(Evaluator.class);
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        segmentCache.updateSegment("segment1", Arrays.asList("bilal@split.io"), new ArrayList<>(), 123);
        RuleBasedSegmentCache ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        EvaluationContext evaluationContext = new EvaluationContext(evaluator, segmentCache, ruleBasedSegmentCache);

        SplitChange change = Json.fromJson(load, SplitChange.class);
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        RuleBasedSegmentsToUpdate ruleBasedSegmentsToUpdate = processRuleBasedSegmentChanges(ruleBasedSegmentParser,
                change.ruleBasedSegments.d);
        ruleBasedSegmentCache.update(ruleBasedSegmentsToUpdate.getToAdd(), null, 123);
        RuleBasedSegmentMatcher matcher = new RuleBasedSegmentMatcher("sample_rule_based_segment");
        HashMap<String, Object> attrib1 =  new HashMap<String, Object>() {{
            put("email", "mauro@split.io");
        }};
        HashMap<String, Object> attrib2 =  new HashMap<String, Object>() {{
            put("email", "bilal@split.io");
        }};
        HashMap<String, Object> attrib3 =  new HashMap<String, Object>() {{
            put("email", "pato@split.io");
        }};
        assertThat(matcher.match("mauro@split.io", null, attrib1, evaluationContext), is(false));
        assertThat(matcher.match("bilal@split.io", null, attrib2, evaluationContext), is(false));
        assertThat(matcher.match("pato@split.io", null, attrib3, evaluationContext), is(true));
    }

    @Test
    public void usingRbsInExcludedTest() throws IOException {
        String load = new String(Files.readAllBytes(Paths.get("src/test/resources/rule_base_segments2.json")), StandardCharsets.UTF_8);
        Evaluator evaluator = Mockito.mock(Evaluator.class);
        SegmentCache segmentCache = new SegmentCacheInMemoryImpl();
        RuleBasedSegmentCache ruleBasedSegmentCache = new RuleBasedSegmentCacheInMemoryImp();
        EvaluationContext evaluationContext = new EvaluationContext(evaluator, segmentCache, ruleBasedSegmentCache);

        SplitChange change = Json.fromJson(load, SplitChange.class);
        RuleBasedSegmentParser ruleBasedSegmentParser = new RuleBasedSegmentParser();
        RuleBasedSegmentsToUpdate ruleBasedSegmentsToUpdate = processRuleBasedSegmentChanges(ruleBasedSegmentParser,
                change.ruleBasedSegments.d);
        ruleBasedSegmentCache.update(ruleBasedSegmentsToUpdate.getToAdd(), null, 123);
        RuleBasedSegmentMatcher matcher = new RuleBasedSegmentMatcher("sample_rule_based_segment");
        HashMap<String, Object> attrib1 =  new HashMap<String, Object>() {{
            put("email", "mauro@split.io");
        }};
        HashMap<String, Object> attrib2 =  new HashMap<String, Object>() {{
            put("email", "bilal@harness.io");
        }};
        assertThat(matcher.match("mauro", null, attrib1, evaluationContext), is(false));
        assertThat(matcher.match("bilal", null, attrib2, evaluationContext), is(true));
    }
}
