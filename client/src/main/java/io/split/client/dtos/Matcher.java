package io.split.client.dtos;

/**
 * A leaf class representing a matcher.
 *
 * @author adil
 */
public class Matcher {
    public KeySelector keySelector;
    public MatcherType matcherType;
    public boolean negate;
    public UserDefinedSegmentMatcherData userDefinedSegmentMatcherData;
    public WhitelistMatcherData whitelistMatcherData;
    public UnaryNumericMatcherData unaryNumericMatcherData;
    public BetweenMatcherData betweenMatcherData;
    public DependencyMatcherData dependencyMatcherData;
    public Boolean booleanMatcherData;
    public String stringMatcherData;
}
