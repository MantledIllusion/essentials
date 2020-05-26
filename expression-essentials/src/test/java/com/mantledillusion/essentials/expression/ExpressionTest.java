package com.mantledillusion.essentials.expression;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

public class ExpressionTest {

    enum Values {
        A, B, C, D
    }

    @Test
    public void testValidateSimple() {
        Expression<Values> expression = Expression.of(Values.A);

        Assertions.assertTrue(expression.evaluate(Values.A::equals));
        Assertions.assertFalse(expression.evaluate(Values.B::equals));
    }

    @Test
    public void testValidateAnd() {
        Expression<Values> expression = Expression.andOf(Values.A, Values.B);

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertFalse(expression.evaluate(Values.A::equals));
        Assertions.assertFalse(expression.evaluate(Values.B::equals));
    }

    @Test
    public void testValidateOr() {
        Expression<Values> expression = Expression.orOf(Values.A, Values.B);

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertTrue(expression.evaluate(Values.A::equals));
        Assertions.assertTrue(expression.evaluate(Values.B::equals));
    }

    @Test
    public void testValidateComplexAndAnd() {
        Expression<Values> expression = Expression.and(Expression.andOf(Values.A, Values.B), Expression.andOf(Values.A, Values.C));

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertFalse(expression.evaluate(Values.A::equals));
        Assertions.assertFalse(expression.evaluate(Values.B::equals));
    }

    @Test
    public void testValidateComplexAndOr() {
        Expression<Values> expression = Expression.and(Expression.orOf(Values.A, Values.B), Expression.orOf(Values.A, Values.C));

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertTrue(expression.evaluate(Values.A::equals));
        Assertions.assertFalse(expression.evaluate(Values.B::equals));
    }

    @Test
    public void testValidateComplexOrAnd() {
        Expression<Values> expression = Expression.or(Expression.andOf(Values.A, Values.B), Expression.andOf(Values.A, Values.C));

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertFalse(expression.evaluate(Values.A::equals));
        Assertions.assertFalse(expression.evaluate(Values.B::equals));
    }

    @Test
    public void testValidateComplexOrOr() {
        Expression<Values> expression = Expression.or(Expression.orOf(Values.A, Values.B), Expression.orOf(Values.A, Values.C));

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertTrue(expression.evaluate(Values.A::equals));
        Assertions.assertTrue(expression.evaluate(Values.B::equals));
    }

    @Test
    public void testValidateDeepAndAnd() {
        Expression<Values> expression = Expression.of(Values.A).and(Expression.andOf(Values.B, Values.C));

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertFalse(expression.evaluate(Values.A::equals));
        Assertions.assertFalse(expression.evaluate(Values.B::equals));
        Assertions.assertFalse(expression.evaluate(Values.C::equals));
    }

    @Test
    public void testValidateDeepAndOr() {
        Expression<Values> expression = Expression.of(Values.A).and(Expression.orOf(Values.B, Values.C));

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertFalse(expression.evaluate(Values.A::equals));
        Assertions.assertFalse(expression.evaluate(Values.B::equals));
        Assertions.assertFalse(expression.evaluate(Values.C::equals));
    }

    @Test
    public void testValidateDeepOrAnd() {
        Expression<Values> expression = Expression.of(Values.A).or(Expression.andOf(Values.B, Values.C));

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertTrue(expression.evaluate(Values.A::equals));
        Assertions.assertFalse(expression.evaluate(Values.B::equals));
        Assertions.assertFalse(expression.evaluate(Values.C::equals));
    }

    @Test
    public void testValidateDeepOrOr() {
        Expression<Values> expression = Expression.of(Values.A).or(Expression.orOf(Values.B, Values.C));

        Assertions.assertTrue(expression.evaluate(Objects::nonNull));
        Assertions.assertTrue(expression.evaluate(Values.A::equals));
        Assertions.assertTrue(expression.evaluate(Values.B::equals));
        Assertions.assertTrue(expression.evaluate(Values.C::equals));
    }

    @Test
    public void testRenderSimple() {
        Assertions.assertEquals("A", Expression.of(Values.A).toString(Values::name));
    }

    @Test
    public void testRenderAnd() {
        Assertions.assertEquals("A && B", Expression.andOf(Values.A, Values.B).toString(Values::name));
    }

    @Test
    public void testRenderOr() {
        Assertions.assertEquals("A || B", Expression.orOf(Values.A, Values.B).toString(Values::name));
    }

    @Test
    public void testRenderComplex() {
        Assertions.assertEquals("(A || B) && (B || C) && (C || D)",
                Expression.and(Expression.orOf(Values.A, Values.B),
                        Expression.orOf(Values.B, Values.C),
                        Expression.orOf(Values.C, Values.D)).toString());
    }

    @Test
    public void testRenderDeep() {
        Assertions.assertEquals("((A && B) || ((B && C) || D)) && (A && C)",
                Expression.andOf(Values.A, Values.B)
                        .or(Expression.andOf(Values.B, Values.C).or(Expression.of(Values.D)))
                        .and(Expression.andOf(Values.A, Values.C)).toString(Values::name));
    }

    @Test
    public void testRenderCustomDelimiters() {
        Assertions.assertEquals("(({A} && {B}) || (({B} && {C}) || {D})) && ({A} && {C})",
                Expression.andOf(Values.A, Values.B)
                        .or(Expression.andOf(Values.B, Values.C).or(Expression.of(Values.D)))
                        .and(Expression.andOf(Values.A, Values.C)).toString(Values::name, "{", "}"));
    }

    @Test
    public void testParseAnd() {
        Assertions.assertEquals("A && B",
                Expression.parse("A && B").toString());
    }

    @Test
    public void testParseOr() {
        Assertions.assertEquals("A || B",
                Expression.parse("A || B").toString());
    }

    @Test
    public void testParseComplex() {
        Assertions.assertEquals("(A || B) && (B || C) && (C || D)",
                Expression.parse("(A || B) && (B || C) && (C || D)").toString());
    }

    @Test
    public void testParseDeep() {
        Assertions.assertEquals("((A && B) || ((B && C) || D)) && (A && C)",
                Expression.parse("((A && B) || ((B && C) || D)) && (A && C)").toString());
    }

    @Test
    public void testParseCustomDelimiters() {
        Assertions.assertEquals("(({A} && {B}) || (({B} && {C}) || {D})) && ({A} && {C})",
                Expression.parse("(({A} && {B}) || (({B} && {C}) || {D})) && ({A} && {C})", "{", "}").toString("{", "}"));
    }
}
