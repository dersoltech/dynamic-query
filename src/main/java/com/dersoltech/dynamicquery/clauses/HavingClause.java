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

/**
 * Represents a single predicate in a HAVING clause.
 */
public final class HavingClause {

    /** The shape of a HAVING predicate. */
    public enum Kind {
        /** A {@code column operator value} comparison. */
        BASIC,
        /** A raw SQL fragment inserted verbatim. */
        RAW
    }

    private final WhereType connector;
    private final Kind kind;
    private final String column;
    private final String operator;
    private final Object value;
    private final String rawSql;

    private HavingClause(WhereType connector, Kind kind,
                         String column, String operator, Object value,
                         String rawSql) {
        this.connector = connector;
        this.kind      = kind;
        this.column    = column;
        this.operator  = operator;
        this.value     = value;
        this.rawSql    = rawSql;
    }

    /**
     * Creates a {@code column operator value} predicate.
     *
     * @param connector how this predicate joins the previous one
     * @param column    the column or aggregate expression, e.g. {@code COUNT(*)}
     * @param operator  the comparison operator, e.g. {@code >}
     * @param value     the value to compare against
     * @return the new predicate
     */
    public static HavingClause basic(WhereType connector,
                                     String column, String operator, Object value) {
        return new HavingClause(connector, Kind.BASIC, column, operator, value, null);
    }

    /**
     * Creates a predicate from a raw SQL fragment.
     *
     * @param connector how this predicate joins the previous one
     * @param rawSql    the SQL fragment, inserted verbatim
     * @return the new predicate
     */
    public static HavingClause raw(WhereType connector, String rawSql) {
        return new HavingClause(connector, Kind.RAW, null, null, null, rawSql);
    }

    /** {@return how this predicate joins the previous one} */
    public WhereType getConnector() { return connector; }
    /** {@return the shape of this predicate} */
    public Kind      getKind()      { return kind; }
    /** {@return the column or expression, or {@code null} for {@link Kind#RAW}} */
    public String    getColumn()    { return column; }
    /** {@return the comparison operator, or {@code null} for {@link Kind#RAW}} */
    public String    getOperator()  { return operator; }
    /** {@return the comparison value, or {@code null} for {@link Kind#RAW}} */
    public Object    getValue()     { return value; }
    /** {@return the raw SQL fragment, or {@code null} unless {@link Kind#RAW}} */
    public String    getRawSql()    { return rawSql; }
}
