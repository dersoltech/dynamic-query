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

    public enum Kind { BASIC, IN, NOT_IN, NULL, NOT_NULL, LIKE, NOT_LIKE, RAW }

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

    public static WhereClause basic(WhereType connector,
                                    String column, String operator, Object value) {
        return new WhereClause(connector, Kind.BASIC, column, operator, value, null, null, null);
    }

    public static WhereClause in(WhereType connector, String column, List<Object> values) {
        return new WhereClause(connector, Kind.IN, column, null, null, List.copyOf(values), null, null);
    }

    public static WhereClause notIn(WhereType connector, String column, List<Object> values) {
        return new WhereClause(connector, Kind.NOT_IN, column, null, null, List.copyOf(values), null, null);
    }

    public static WhereClause isNull(WhereType connector, String column) {
        return new WhereClause(connector, Kind.NULL, column, null, null, null, null, null);
    }

    public static WhereClause isNotNull(WhereType connector, String column) {
        return new WhereClause(connector, Kind.NOT_NULL, column, null, null, null, null, null);
    }

    public static WhereClause like(WhereType connector,
                                   String column, String pattern, Character escape) {
        return new WhereClause(connector, Kind.LIKE, column, null, pattern, null, null, escape);
    }

    public static WhereClause notLike(WhereType connector,
                                      String column, String pattern, Character escape) {
        return new WhereClause(connector, Kind.NOT_LIKE, column, null, pattern, null, null, escape);
    }

    public static WhereClause raw(WhereType connector, String rawSql) {
        return new WhereClause(connector, Kind.RAW, null, null, null, null, rawSql, null);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public WhereType getConnector() { return connector; }
    public Kind      getKind()      { return kind; }
    public String    getColumn()    { return column; }
    public String    getOperator()  { return operator; }
    public Object    getValue()     { return value; }
    public List<Object> getValues() { return values; }
    public String    getRawSql()    { return rawSql; }
    public Character getEscape()    { return escape; }
}
