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
package com.dersoltech.dynamicquery.clauses;

import com.dersoltech.dynamicquery.enums.WhereType;

import java.util.List;

/**
 * Represents a single predicate in a WHERE clause.
 *
 * <p>Use the static factory methods instead of constructing directly.
 */
public final class WhereClause {

    /** The shape of a WHERE predicate. */
    public enum Kind {
        /** A {@code column operator value} comparison. */
        BASIC,
        /** {@code column IN (...)}. */
        IN,
        /** {@code column NOT IN (...)}. */
        NOT_IN,
        /** {@code column IS NULL}. */
        NULL,
        /** {@code column IS NOT NULL}. */
        NOT_NULL,
        /** {@code column LIKE pattern}. */
        LIKE,
        /** {@code column NOT LIKE pattern}. */
        NOT_LIKE,
        /** A raw SQL fragment inserted verbatim. */
        RAW
    }

    private final WhereType connector;
    private final Kind kind;

    // BASIC / LIKE / NOT_LIKE
    private final String column;
    private final String operator;
    private final Object value;

    // IN / NOT_IN
    private final List<Object> values;

    // RAW
    private final String rawSql;

    // LIKE / NOT_LIKE — null means no ESCAPE clause
    private final Character escape;

    // -------------------------------------------------------------------------
    // Private constructor — callers use factories below
    // -------------------------------------------------------------------------

    private WhereClause(WhereType connector, Kind kind,
                        String column, String operator, Object value,
                        List<Object> values, String rawSql, Character escape) {
        this.connector = connector;
        this.kind      = kind;
        this.column    = column;
        this.operator  = operator;
        this.value     = value;
        this.values    = values;
        this.rawSql    = rawSql;
        this.escape    = escape;
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code column operator value} predicate.
     *
     * @param connector how this predicate joins the previous one
     * @param column    the column name
     * @param operator  the comparison operator, e.g. {@code =}
     * @param value     the value to compare against
     * @return the new predicate
     */
    public static WhereClause basic(WhereType connector,
                                    String column, String operator, Object value) {
        return new WhereClause(connector, Kind.BASIC, column, operator, value, null, null, null);
    }

    /**
     * Creates a {@code column IN (...)} predicate.
     *
     * @param connector how this predicate joins the previous one
     * @param column    the column name
     * @param values    the values to match; copied defensively
     * @return the new predicate
     */
    public static WhereClause in(WhereType connector, String column, List<Object> values) {
        return new WhereClause(connector, Kind.IN, column, null, null, List.copyOf(values), null, null);
    }

    /**
     * Creates a {@code column NOT IN (...)} predicate.
     *
     * @param connector how this predicate joins the previous one
     * @param column    the column name
     * @param values    the values to exclude; copied defensively
     * @return the new predicate
     */
    public static WhereClause notIn(WhereType connector, String column, List<Object> values) {
        return new WhereClause(connector, Kind.NOT_IN, column, null, null, List.copyOf(values), null, null);
    }

    /**
     * Creates a {@code column IS NULL} predicate.
     *
     * @param connector how this predicate joins the previous one
     * @param column    the column name
     * @return the new predicate
     */
    public static WhereClause isNull(WhereType connector, String column) {
        return new WhereClause(connector, Kind.NULL, column, null, null, null, null, null);
    }

    /**
     * Creates a {@code column IS NOT NULL} predicate.
     *
     * @param connector how this predicate joins the previous one
     * @param column    the column name
     * @return the new predicate
     */
    public static WhereClause isNotNull(WhereType connector, String column) {
        return new WhereClause(connector, Kind.NOT_NULL, column, null, null, null, null, null);
    }

    /**
     * Creates a {@code column LIKE pattern} predicate.
     *
     * @param connector how this predicate joins the previous one
     * @param column    the column name
     * @param pattern   the LIKE pattern
     * @param escape    the ESCAPE character, or {@code null} for no ESCAPE clause
     * @return the new predicate
     */
    public static WhereClause like(WhereType connector,
                                   String column, String pattern, Character escape) {
        return new WhereClause(connector, Kind.LIKE, column, null, pattern, null, null, escape);
    }

    /**
     * Creates a {@code column NOT LIKE pattern} predicate.
     *
     * @param connector how this predicate joins the previous one
     * @param column    the column name
     * @param pattern   the LIKE pattern
     * @param escape    the ESCAPE character, or {@code null} for no ESCAPE clause
     * @return the new predicate
     */
    public static WhereClause notLike(WhereType connector,
                                      String column, String pattern, Character escape) {
        return new WhereClause(connector, Kind.NOT_LIKE, column, null, pattern, null, null, escape);
    }

    /**
     * Creates a predicate from a raw SQL fragment.
     *
     * @param connector how this predicate joins the previous one
     * @param rawSql    the SQL fragment, inserted verbatim
     * @return the new predicate
     */
    public static WhereClause raw(WhereType connector, String rawSql) {
        return new WhereClause(connector, Kind.RAW, null, null, null, null, rawSql, null);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** {@return how this predicate joins the previous one} */
    public WhereType getConnector() { return connector; }
    /** {@return the shape of this predicate} */
    public Kind      getKind()      { return kind; }
    /** {@return the column name, or {@code null} for {@link Kind#RAW}} */
    public String    getColumn()    { return column; }
    /** {@return the comparison operator; set only for {@link Kind#BASIC}} */
    public String    getOperator()  { return operator; }
    /** {@return the comparison value or LIKE pattern, or {@code null} if not applicable} */
    public Object    getValue()     { return value; }
    /** {@return the IN / NOT IN values, or {@code null} if not applicable} */
    public List<Object> getValues() { return values; }
    /** {@return the raw SQL fragment, or {@code null} unless {@link Kind#RAW}} */
    public String    getRawSql()    { return rawSql; }
    /** {@return the LIKE ESCAPE character, or {@code null} for no ESCAPE clause} */
    public Character getEscape()    { return escape; }
}
