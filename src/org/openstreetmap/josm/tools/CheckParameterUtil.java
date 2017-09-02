// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.text.MessageFormat;
import java.util.function.Function;

/**
 * This utility class provides a collection of static helper methods for checking
 * parameters at run-time.
 * @since 2711
 */
public final class CheckParameterUtil {

    private CheckParameterUtil() {
        // Hide default constructor for utils classes
    }

    public static <T> void ensureValid(T obj, String parameterName, Function<T, Boolean> check) {
        ensureParameterNotNull(obj, parameterName);
        if (!check.apply(obj))
            throw new IllegalArgumentException(
                    MessageFormat.format("Parameter value ''{0}'' of type {1} is invalid, got ''{2}''",
                            parameterName,
                            obj.getClass().getCanonicalName(),
                            obj));
    }

    public static <T> void ensureValid(T obj, String parameterName, String conditionMsg, Function<T, Boolean> check) {
        ensureParameterNotNull(obj, parameterName);
        if (!check.apply(obj))
            throw new IllegalArgumentException(
                    MessageFormat.format("Parameter value ''{0}'' of type {1} is invalid, violated condition: ''{2}'', value is: ''{3}''",
                            parameterName,
                            obj.getClass().getCanonicalName(),
                            conditionMsg,
                            obj));
    }

    /**
     * Ensures a parameter is not {@code null}
     * @param value The parameter to check
     * @param parameterName The parameter name
     * @throws IllegalArgumentException if the parameter is {@code null}
     */
    public static void ensureParameterNotNull(Object value, String parameterName) {
        if (value == null)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' must not be null", parameterName));
    }

    /**
     * Ensures a parameter is not {@code null}. Can find line number in the stack trace, so parameter name is optional
     * @param value The parameter to check
     * @throws IllegalArgumentException if the parameter is {@code null}
     * @since 3871
     */
    public static void ensureParameterNotNull(Object value) {
        if (value == null)
            throw new IllegalArgumentException("Parameter must not be null");
    }

    /**
     * Ensures that the condition {@code condition} holds.
     * @param condition The condition to check
     * @param message error message
     * @throws IllegalArgumentException if the condition does not hold
     */
    public static void ensureThat(boolean condition, String message) {
        if (!condition)
            throw new IllegalArgumentException(message);
    }
}
