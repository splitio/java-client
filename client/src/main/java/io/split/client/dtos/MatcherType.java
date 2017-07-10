package io.split.client.dtos;

/**
 * Created by adilaijaz on 5/28/15.
 */
public enum MatcherType {
    ALL_KEYS,
    IN_SEGMENT,
    WHITELIST,

    /* Numeric Matcher */
    EQUAL_TO,
    GREATER_THAN_OR_EQUAL_TO,
    LESS_THAN_OR_EQUAL_TO,
    BETWEEN,

    /* Set Matcher */
    EQUAL_TO_SET,
    CONTAINS_ANY_OF_SET,
    CONTAINS_ALL_OF_SET,
    PART_OF_SET,

    /* String Matcher */
    STARTS_WITH,
    ENDS_WITH,
    CONTAINS_STRING,
    MATCHES_STRING,

    /* Boolean Matcher */
    EQUAL_TO_BOOLEAN,

    /* Dependency Matcher */
    IN_SPLIT_TREATMENT
}
