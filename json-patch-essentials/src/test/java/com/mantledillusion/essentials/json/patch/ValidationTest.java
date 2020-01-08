package com.mantledillusion.essentials.json.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.mantledillusion.essentials.json.patch.model.Patch;
import com.mantledillusion.essentials.json.patch.model.PatchOperation;
import com.mantledillusion.essentials.json.patch.validation.ValidPatch;
import com.mantledillusion.essentials.json.patch.validation.ValidPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationTest {

    @Test
    public void testMissingOp() {
        assertViolation(new Patch(null, "/path", new TextNode("value")), NotNull.class);
    }

    @Test
    public void testMissingPath() {
        assertViolation(new Patch(PatchOperation.add,  null, new TextNode("value")), NotNull.class);
    }

    @Test
    public void testInvalidPath() {
        assertViolation(new Patch(PatchOperation.remove,  ""), ValidPath.class);
        assertViolation(new Patch(PatchOperation.remove,  "missingSlash"), ValidPath.class);
        assertViolation(new Patch(PatchOperation.remove,  "/endingslash/"), ValidPath.class);
        assertViolation(new Patch(PatchOperation.remove,  "//doubleSlash"), ValidPath.class);
        assertViolation(new Patch(PatchOperation.remove,  "/0leadingNumber"), ValidPath.class);
        assertViolation(new Patch(PatchOperation.remove,  "/weirdCharacter$"), ValidPath.class);
    }

    @Test
    public void testMissingFromForOp() {
        for (PatchOperation operation: Arrays.stream(PatchOperation.values()).filter(op -> op.isRequiresFrom()).collect(Collectors.toList())) {
            assertViolation(new Patch(operation,  null,"/path"), ValidPatch.class);
        }
    }

    @Test
    public void testMissingValueForOp() {
        for (PatchOperation operation: Arrays.stream(PatchOperation.values()).filter(op -> op.isRequiresValue()).collect(Collectors.toList())) {
            assertViolation(new Patch(operation, "/path", (JsonNode) null), ValidPatch.class);
        }
    }

    private <T> void assertViolation(T request, Class<? extends Annotation> validationAnnotationClass) {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();

        Set<ConstraintViolation<T>> violations = validator.validate(request);
        Assertions.assertFalse(violations.isEmpty());
        Assertions.assertEquals(1, violations.size());

        ConstraintViolation<T> violation = violations.iterator().next();
        Assertions.assertEquals(validationAnnotationClass, violation.getConstraintDescriptor().getAnnotation().annotationType());
    }
}
