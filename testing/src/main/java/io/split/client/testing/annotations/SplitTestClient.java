package io.split.client.testing.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SplitTestClient {
    SplitScenario[] scenarios() default {};
}
