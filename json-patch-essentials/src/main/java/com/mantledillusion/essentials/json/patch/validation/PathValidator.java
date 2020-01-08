package com.mantledillusion.essentials.json.patch.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * {@link ConstraintValidator} that checks an annotated {@link String} whether their value is a Valid JSON path with
 * the pattern {@link ValidPath#REGEX_PATH}
 * <p>
 * Null values evaluate true; combine with {@link javax.validation.constraints.NotNull} if needed.
 */
public class PathValidator implements ConstraintValidator<ValidPath, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.matches(ValidPath.REGEX_PATH);
    }
}
