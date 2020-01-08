package com.mantledillusion.essentials.json.patch.validation;

import com.mantledillusion.essentials.json.patch.model.Patch;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * {@link ConstraintValidator} that checks {@link Patch} instances whether their 'from' and 'value' field are filled
 * according to their operation.
 */
public class PatchValidator implements ConstraintValidator<ValidPatch, Patch> {

    @Override
    public boolean isValid(Patch value, ConstraintValidatorContext context) {
        return value.getOp().isFulfilledBy(value.getFrom(), value.getValue());
    }
}
