package com.dersoltech.dynamicquery.util;

import java.util.Collection;
import java.util.StringJoiner;

/**
 * Formats Java values into their SQL literal representation.
 */
public final class SqlFormatter {

    /** Escape character used by the LIKE helper methods ({@code ESCAPE '\'}). */
    public static final char LIKE_ESCAPE = '\\';

    private SqlFormatter() {}

    /**
     * Escapes LIKE wildcards so {@code value} matches literally.
     *
     * <p>Prefixes {@link #LIKE_ESCAPE}, {@code %} and {@code _} with
     * {@link #LIKE_ESCAPE}. Example: {@code 50%_off} → {@code 50\%\_off}
     */
    public static String escapeLike(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c == LIKE_ESCAPE || c == '%' || c == '_') sb.append(LIKE_ESCAPE);
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Converts a Java value to a SQL literal string.
     *
     * <ul>
     *   <li>{@code null}        → {@code NULL}</li>
     *   <li>{@link String}      → {@code 'value'} (single-quotes, inner single-quotes escaped)</li>
     *   <li>{@link Boolean}     → {@code TRUE} / {@code FALSE}</li>
     *   <li>{@link Number}      → the number as-is</li>
     *   <li>{@link Enum}        → {@code 'ENUM_NAME'}</li>
     *   <li>Everything else     → {@code toString()} wrapped in single quotes</li>
     * </ul>
     */
    public static String format(Object value) {
        return switch (value) {
            case null -> "NULL";
            case String s -> "'" + s.replace("'", "''") + "'";
            case Boolean b -> b ? "TRUE" : "FALSE";
            case Number number -> value.toString();
            case Enum<?> e -> "'" + e.name() + "'";
            default -> "'" + value.toString().replace("'", "''") + "'";
        };
    }

    /**
     * Formats a collection of values as a parenthesized, comma-separated SQL list.
     *
     * <p>Example: {@code (1, 2, 3)} or {@code ('a', 'b')}
     */
    public static String formatList(Collection<?> values) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (Object v : values) {
            joiner.add(format(v));
        }
        return joiner.toString();
    }

    /**
     * Returns a parenthesized list of {@code count} {@code ?} placeholders.
     *
     * <p>Example: {@code placeholders(3)} → {@code (?, ?, ?)}
     */
    public static String placeholders(int count) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (int i = 0; i < count; i++) joiner.add("?");
        return joiner.toString();
    }
}
