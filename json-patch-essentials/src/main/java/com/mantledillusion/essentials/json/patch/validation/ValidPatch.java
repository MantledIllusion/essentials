package com.mantledillusion.essentials.json.patch.validation;

import com.mantledillusion.essentials.json.patch.model.Patch;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for {@link Patch}es that need to be checked whether their 'from' and 'value' field are filled according
 * to their operation.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PatchValidator.class)
public @interface ValidPatch {

    String message() default "The patches from/value field is not filled according to the operation";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
