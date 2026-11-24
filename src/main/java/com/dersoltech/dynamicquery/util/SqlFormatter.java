/*
 * Copyright 2026 David Robertson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     *
     * @param value the raw text to match literally
     * @return the text with LIKE wildcards escaped
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
     *
     * @param value the value to format; may be {@code null}
     * @return the SQL literal
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
     *
     * @param values the values to format
     * @return the parenthesized SQL list
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
     *
     * @param count the number of placeholders
     * @return the parenthesized placeholder list
     */
    public static String placeholders(int count) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (int i = 0; i < count; i++) joiner.add("?");
        return joiner.toString();
    }
}
