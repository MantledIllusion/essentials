package com.mantledillusion.essentials.json.patch.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for {@link String}s that need to be checked whether their value is a Valid JSON path with the pattern
 * {@link #REGEX_PATH}
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PathValidator.class)
public @interface ValidPath {

    /**
     * A regular expression for non-floating point numbers.
     */
    String REGEX_NUMERIC = "(0|([1-9]+[0-9]*))";

    /**
     * A regular expression for alphanumeric {@link String}s that simply requires them to begin with a letter.
     */
    String REGEX_ALPHANUMERIC = "([a-zA-Z_-]+[a-zA-Z0-9_-]*)";

    /**
     * A regular expression for JSON paths that requires such paths to contain at least one segment beginning with a
     * forward slash followed by a number or alphanumeric sequence.
     */
    String REGEX_PATH = "(\\/("+REGEX_NUMERIC+"|"+REGEX_ALPHANUMERIC+"))+";

    String message() default "A node path does not match the pattern "+REGEX_PATH;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
