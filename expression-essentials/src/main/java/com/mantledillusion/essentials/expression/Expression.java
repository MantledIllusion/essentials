package com.mantledillusion.essentials.expression;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A totally generic, boolean based {@link Expression} of any type.
 * <p>
 * Instantiate using either...<br>
 * - {@link #of(Object)}<br>
 * - {@link #andOf(Object[])} / {@link #andOf(Collection)}<br>
 * - {@link #and(Expression[])} / {@link #and(Collection)}<br>
 * - {@link #orOf(Object[])} / {@link #orOf(Collection)}<br>
 * - {@link #or(Expression[])} / {@link #or(Collection)}<br>
 * <br>
 * ...and then append to the existing instance using...<br>
 * <br>
 * - {@link #and(Expression)}<br>
 * - {@link #or(Expression)}<br>
 *
 * @param <V> The value type.
 */
public class Expression<V> {

    private interface ExpressionRenderer<V> {

        String render(Function<V, String> vR, String sD, String eD);
    }

    private static final String DELIMITER_MATCHER = "[^\\s\\(\\)\\&\\|]*";

    private static final String FUNCTION_AND = "&&";
    private static final String FUNCTION_OR = "||";

    private static final String DEFAULT_START_DELIMITER = "";
    private static final String DEFAULT_END_DELIMITER = "";

    private final boolean isComposition;
    private final Predicate<Predicate<V>> predicate;
    private final ExpressionRenderer<V> renderer;

    private Expression(boolean isComposition, Predicate<Predicate<V>> predicate, ExpressionRenderer<V> renderer) {
        this.isComposition = isComposition;
        this.predicate = predicate;
        this.renderer = renderer;
    }

    // #################################################################################################################
    // ########################################### DYNAMIC CONCATENATION ###############################################
    // #################################################################################################################

    /**
     * Creates a new {@link Expression} based on <code>this</code> one, that evaluates as: <code>this && other</code>
     *
     * @param other The other {@link Expression}; might <b>not</b> be null.
     * @return A new {@link Expression}, never null
     */
    public Expression<V> and(Expression<V> other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot concatenate a null other expression");
        }
        return new Expression<>(true, predicate -> Expression.this.evaluate(predicate) && other.evaluate(predicate),
                (vR, sD, eD) -> Expression.this.toString(vR, sD, eD, true)  + ' ' + FUNCTION_AND + ' ' + other.toString(vR, sD, eD, true));
    }

    /**
     * Creates a new {@link Expression} based on <code>this</code> one, that evaluates as: <code>this || other</code>
     *
     * @param other The other {@link Expression}; might <b>not</b> be null.
     * @return A new {@link Expression}, never null
     */
    public Expression<V> or(Expression<V> other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot concatenate a null other expression");
        }
        return new Expression<>(true, predicate -> Expression.this.evaluate(predicate) || other.evaluate(predicate),
                (vR, sD, eD) -> Expression.this.toString(vR, sD, eD, true)  + ' ' + FUNCTION_OR + ' ' + other.toString(vR, sD, eD, true));
    }

    // #################################################################################################################
    // ################################################# VALIDATION ####################################################
    // #################################################################################################################

    /**
     * Will evaluate this {@link Expression}.
     * <p>
     * The given {@link Predicate} will be called with all of the values of this expression to evaluate if they, on
     * their own, equal true or false.
     *
     * @param predicate The predicate to use; might <b>not</b> be null.
     * @return True if this {@link Expression} evaluates true, false otherwise.
     */
    public boolean evaluate(Predicate<V> predicate) {
        return this.predicate.test(predicate);
    }

    // #################################################################################################################
    // ################################################## TO STRING ####################################################
    // #################################################################################################################

    /**
     * Will render this {@link Expression} using:<br>
     * - {@link Objects#toString(Object)} as a renderer<br>
     * - {@value #DEFAULT_START_DELIMITER} as the start delimiter<br>
     * - {@value #DEFAULT_END_DELIMITER} as the end delimiter<br>
     */
    @Override
    public String toString() {
        return toString(Objects::toString, DEFAULT_START_DELIMITER, DEFAULT_END_DELIMITER, false);
    }

    /**
     * Will render this {@link Expression} using:<br>
     * - {@value #DEFAULT_START_DELIMITER} as the start delimiter<br>
     * - {@value #DEFAULT_END_DELIMITER} as the end delimiter<br>
     *
     * @param valueRenderer The renderer to use; might <b>not</b> be null.
     */
    public String toString(Function<V, String> valueRenderer) {
        return toString(valueRenderer, DEFAULT_START_DELIMITER, DEFAULT_END_DELIMITER, false);
    }

    /**
     * Will render this {@link Expression} using:<br>
     * - {@link Objects#toString(Object)} as a renderer<br>
     *
     * @param startDelimiter The start delimiter to use; might <b>not</b> be null or of the characters [\w()&|].
     * @param endDelimiter The end delimiter to use; might <b>not</b> be null or of the characters [\w()&|].
     */
    public String toString(String startDelimiter, String endDelimiter) {
        return toString(Objects::toString, startDelimiter, endDelimiter, false);
    }

    /**
     * Will render this {@link Expression}.
     *
     * @param valueRenderer The renderer to use; might <b>not</b> be null.
     * @param startDelimiter The start delimiter to use; might <b>not</b> be null or of the characters [\w()&|].
     * @param endDelimiter The end delimiter to use; might <b>not</b> be null or of the characters [\w()&|].
     */
    public String toString(Function<V, String> valueRenderer, String startDelimiter, String endDelimiter) {
        return toString(valueRenderer, startDelimiter, endDelimiter, false);
    }

    private String toString(Function<V, String> vR, String sD, String eD, boolean brace) {
        if (vR == null) {
            throw new IllegalArgumentException("Cannot render without a value renderer.");
        } else if (sD == null || !sD.matches(DELIMITER_MATCHER)) {
            throw new IllegalArgumentException("The start delimiter '" + sD + "' is no valid delimiter.");
        } else if (eD == null || !eD.matches(DELIMITER_MATCHER)) {
            throw new IllegalArgumentException("The end delimiter '" + eD + "' is no valid delimiter.");
        }
        return (brace && this.isComposition ? '(' : "") +
                this.renderer.render(vR, sD, eD) +
                (brace && this.isComposition ? ')' : "");
    }

    // #################################################################################################################
    // ############################################ STATIC CONCATENATION ###############################################
    // #################################################################################################################

    /**
     * Creates a new {@link Expression}.
     *
     * @param <V> The value type.
     * @param value The instance to express; might be null.
     * @return A new {@link Expression}, never null
     */
    public static <V> Expression<V> of(V value) {
        return new Expression<>(false, predicate -> predicate.test(value),
                (vR, sD, eD) -> sD + vR.apply(value) + eD);
    }

    /**
     *Creates a new && {@link Expression}.
     *
     * @param <V> The value type.
     * @param values The instances to express and concatenate; might be null.
     * @return A new {@link Expression}, never null
     */
    @SafeVarargs
    public static <V> Expression<V> andOf(V... values) {
        return andOf(Arrays.asList(values));
    }

    /**
     *Creates a new && {@link Expression}.
     *
     * @param <V> The value type.
     * @param values The instances to express and concatenate; might <b>not</b> be null.
     * @return A new {@link Expression}, never null
     */
    public static <V> Expression<V> andOf(Collection<V> values) {
        return and(values.stream().map(Expression::of).collect(Collectors.toList()));
    }

    /**
     *Creates a new && {@link Expression}.
     *
     * @param <V> The value type.
     * @param expressions The expressions to concatenate; might be null.
     * @return A new {@link Expression}, never null
     */
    @SafeVarargs
    public static <V> Expression<V> and(Expression<V>... expressions) {
        return and(Arrays.asList(expressions));
    }

    /**
     *Creates a new && {@link Expression}.
     *
     * @param <V> The value type.
     * @param expressions The expressions to concatenate; might <b>not</b> be null.
     * @return A new {@link Expression}, never null
     */
    public static <V> Expression<V> and(Collection<Expression<V>> expressions) {
        if (expressions == null) {
            throw new IllegalArgumentException("Cannot concatenate a null expression collection.");
        }
        return new Expression<>(expressions.size() > 1, predicate -> expressions.stream().allMatch(ex -> ex.evaluate(predicate)),
                (vR, sD, eD) -> renderExpressions(vR, sD, eD, FUNCTION_AND, expressions));
    }

    /**
     *Creates a new || {@link Expression}.
     *
     * @param <V> The value type.
     * @param values The instances to express and concatenate; might be null.
     * @return A new {@link Expression}, never null
     */
    @SafeVarargs
    public static <V> Expression<V> orOf(V... values) {
        return orOf(Arrays.asList(values));
    }

    /**
     *Creates a new || {@link Expression}.
     *
     * @param <V> The value type.
     * @param values The instances to express and concatenate; might <b>not</b> be null.
     * @return A new {@link Expression}, never null
     */
    public static <V> Expression<V> orOf(Collection<V> values) {
        return or(values.stream().map(Expression::of).collect(Collectors.toList()));
    }

    /**
     *Creates a new || {@link Expression}.
     *
     * @param <V> The value type.
     * @param expressions The expressions to concatenate; might be null.
     * @return A new {@link Expression}, never null
     */
    @SafeVarargs
    public static <V> Expression<V> or(Expression<V>... expressions) {
        return or(Arrays.asList(expressions));
    }

    /**
     *Creates a new || {@link Expression}.
     *
     * @param <V> The value type.
     * @param expressions The expressions to concatenate; might <b>not</b> be null.
     * @return A new {@link Expression}, never null
     */
    public static <V> Expression<V> or(Collection<Expression<V>> expressions) {
        if (expressions == null) {
            throw new IllegalArgumentException("Cannot concatenate a null expression collection.");
        }
        return new Expression<>(expressions.size() > 1, predicate -> expressions.stream().anyMatch(ex -> ex.evaluate(predicate)),
                (vR, sD, eD) -> renderExpressions(vR, sD, eD, FUNCTION_OR, expressions));
    }

    private static <V> String renderExpressions(Function<V, String> vR, String sD, String eD, String function, Collection<Expression<V>> expressions) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Expression<V> expression: expressions) {
            sb.append(expression.toString(vR, sD, eD, true));
            if (i < expressions.size()-1) {
                sb.append(' ').append(function).append(' ');
            }
            i++;
        }
        return sb.toString();
    }

    // #################################################################################################################
    // ################################################# FROM STRING ###################################################
    // #################################################################################################################

    /**
     * Parses the given {@link String} into an {@link Expression} using:<br>
     * - no parser<br>
     * - {@value #DEFAULT_START_DELIMITER} as the start delimiter<br>
     * - {@value #DEFAULT_END_DELIMITER} as the end delimiter<br>
     *
     * @param expression The expression to parse; might <b>not</b> be null.
     * @return An {@link Expression}, never null
     */
    public static Expression<String> parse(String expression) {
        return parse(expression, s -> s, DEFAULT_START_DELIMITER, DEFAULT_END_DELIMITER);
    }

    /**
     * Parses the given {@link String} into an {@link Expression} using:<br>
     * - no parser<br>
     *
     * @param expression The expression to parse; might <b>not</b> be null.
     * @param startDelimiter The start delimiter to use; might <b>not</b> be null or of the characters [\w()&|].
     * @param endDelimiter The end delimiter to use; might <b>not</b> be null or of the characters [\w()&|].
     * @return An {@link Expression}, never null
     */
    public static Expression<String> parse(String expression, String startDelimiter, String endDelimiter) {
        return parse(expression, s -> s, startDelimiter, endDelimiter);
    }

    /**
     * Parses the given {@link String} into an {@link Expression} using:<br>
     * - {@value #DEFAULT_START_DELIMITER} as the start delimiter<br>
     * - {@value #DEFAULT_END_DELIMITER} as the end delimiter<br>
     *
     * @param expression The expression to parse; might <b>not</b> be null.
     * @param parser The parser to use; might <b>not</b> be null.
     * @return An {@link Expression}, never null
     */
    public static <V> Expression<V> parse(String expression, Function<String, V> parser) {
        return parse(expression, parser, DEFAULT_START_DELIMITER, DEFAULT_END_DELIMITER);
    }

    /**
     * Parses the given {@link String} into an {@link Expression}.
     *
     * @param expression The expression to parse; might <b>not</b> be null.
     * @param parser The parser to use; might <b>not</b> be null.
     * @param startDelimiter The start delimiter to use; might <b>not</b> be null or of the characters [\w()&|].
     * @param endDelimiter The end delimiter to use; might <b>not</b> be null or of the characters [\w()&|].
     * @return An {@link Expression}, never null
     */
    public static <V> Expression<V> parse(String expression, Function<String, V> parser, String startDelimiter, String endDelimiter) {
        return parse(parser, startDelimiter, endDelimiter, expression);
    }

    private static <V> Expression<V> parse(Function<String, V> vP, String sD, String eD, String expression) {
        if (vP == null) {
            throw new IllegalArgumentException("Cannot render without a value renderer.");
        } else if (sD == null || !sD.matches(DELIMITER_MATCHER)) {
            throw new IllegalArgumentException("The start delimiter '" + sD + "' is no valid delimiter.");
        } else if (eD == null || !eD.matches(DELIMITER_MATCHER)) {
            throw new IllegalArgumentException("The end delimiter '" + eD + "' is no valid delimiter.");
        }

        List<String> andParts = splitExpression(expression, FUNCTION_AND);
        List<String> orParts = splitExpression(expression, FUNCTION_OR);

        List<String> expressionParts;
        if (andParts.isEmpty()) {
            if (orParts.isEmpty()) {
                expressionParts = Collections.singletonList(expression);
            } else {
                expressionParts = orParts;
            }
        } else {
            if (orParts.isEmpty()) {
                expressionParts = andParts;
            } else {
                throw new IllegalArgumentException("The expression '" + expression + "' contains both " +
                        FUNCTION_AND + " and " + FUNCTION_OR + " operators; automatic order of operations is not " +
                        "supported, please provide brackets!");
            }
        }

        List<Expression<V>> expressions = new ArrayList<>();
        for (String expressionPart: expressionParts) {
            int bracketStartIdx = expressionPart.indexOf('(');
            int bracketEndIdx = expressionPart.lastIndexOf(')');

            if (bracketStartIdx != -1) {
                if (bracketEndIdx != -1) {
                    expressions.add(parse(vP, sD, eD, expressionPart.substring(bracketStartIdx+1, bracketEndIdx)));
                } else {
                    throw new IllegalArgumentException("The expression '" + expression +
                            "' does not contain a valid bracketing format.");
                }
            } else {
                if (bracketEndIdx != -1) {
                    throw new IllegalArgumentException("The expression '" + expression +
                            "' does not contain a valid bracketing format.");
                } else {
                    expressions.add(Expression.of(parseBracketlessValue(vP, sD, eD, expressionPart)));
                }
            }
        }

        return expressions.size() == 1 ? expressions.iterator().next() : (andParts.isEmpty() ?
                Expression.or(expressions) : Expression.and(expressions));
    }

    private static List<String> splitExpression(String expressionPart, String function) {
        List<String> expressionSubParts = new ArrayList<>();

        // FIND ALL FUNCTION OCCURRENCES
        Set<Integer> functionIndexes = findFunctionIndexes(expressionPart, function);

        // THROW OUT FUNCTION OCCURRENCES NOT ON THE BASE LEVEL
        int level = 0;
        for (int i=0; i<expressionPart.length(); i++) {
            char c = expressionPart.charAt(i);
            if (c == '(') {
                level++;
            } else if (c == ')') {
                level--;
            } else if (functionIndexes.contains(i) && level > 0) {
                functionIndexes.remove(i);
            }
        }

        if (!functionIndexes.isEmpty()) {
            // LIST BASE LEVEL FUNCTION OCCURRENCES BY INDEX
            List<Integer> sortedFunctionIndexes = new ArrayList<>(functionIndexes);
            sortedFunctionIndexes.sort(Comparator.comparingInt(i -> i));

            // SPLIT INTO PARTS BY BASE LEVEL FUNCTION OCCURRENCES
            int baseIndex = 0;
            for (Integer idx: sortedFunctionIndexes) {
                expressionSubParts.add(expressionPart.substring(baseIndex, idx));
                baseIndex = idx+function.length();
            }
            expressionSubParts.add(expressionPart.substring(baseIndex));
        }

        return expressionSubParts;
    }

    private static Set<Integer> findFunctionIndexes(String expression, String function) {
        Set<Integer> indexes = new HashSet<>();
        int cutOfflength = 0;
        while (true) {
            int funcIdx = expression.indexOf(function);
            if (funcIdx != -1) {
                indexes.add(cutOfflength+funcIdx);
                cutOfflength += funcIdx+2;
                expression = expression.substring(funcIdx+function.length());
            } else {
                return indexes;
            }
        }
    }

    private static <V> V parseBracketlessValue(Function<String, V> vP, String sD, String eD, String expression) {
        if (sD.isEmpty() && eD.isEmpty()) {
            return vP.apply(expression.trim());
        }

        int openIdx = expression.indexOf(sD);
        int closeIdx = expression.lastIndexOf(eD);

        if (openIdx == -1) {
            throw new IllegalArgumentException("The start delimiter '" + sD +
                    "' could not be found in the expression '" + expression + "'");
        } else if (closeIdx == -1) {
            throw new IllegalArgumentException("The end delimiter '" + eD +
                    "' could not be found in the expression '" + expression + "'");
        }

        return vP.apply(expression.substring(openIdx+sD.length(), closeIdx));
    }
}
