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

import com.dersoltech.dynamicquery.enums.JoinType;

/**
 * Represents a single JOIN clause.
 *
 * <p>When {@code softDeleteColumn} is non-null the query builder will append
 * {@code AND table.column IS NULL} to the ON condition (unless the query is
 * running in {@code withDeleted} mode).
 */
public final class JoinClause {

    private final JoinType type;
    private final String   table;
    private final String   first;
    private final String   operator;
    private final String   second;
    private final String   softDeleteColumn; // null → no soft-delete filter on this join

    /**
     * Constructs a join with no soft-delete awareness.
     *
     * @param type     the join type
     * @param table    the table to join
     * @param first    the left-hand column of the ON condition
     * @param operator the ON condition operator, e.g. {@code =}
     * @param second   the right-hand column of the ON condition
     */
    public JoinClause(JoinType type, String table,
                      String first, String operator, String second) {
        this(type, table, first, operator, second, null);
    }

    /**
     * Constructs a join that optionally carries a soft-delete column.
     *
     * @param type             the join type
     * @param table            the table to join
     * @param first            the left-hand column of the ON condition
     * @param operator         the ON condition operator, e.g. {@code =}
     * @param second           the right-hand column of the ON condition
     * @param softDeleteColumn the joined table's soft-delete column, or {@code null} for none
     */
    public JoinClause(JoinType type, String table,
                      String first, String operator, String second,
                      String softDeleteColumn) {
        this.type             = type;
        this.table            = table;
        this.first            = first;
        this.operator         = operator;
        this.second           = second;
        this.softDeleteColumn = softDeleteColumn;
    }

    /** {@return the join type} */
    public JoinType getType()             { return type; }
    /** {@return the joined table} */
    public String   getTable()            { return table; }
    /** {@return the left-hand column of the ON condition} */
    public String   getFirst()            { return first; }
    /** {@return the ON condition operator} */
    public String   getOperator()         { return operator; }
    /** {@return the right-hand column of the ON condition} */
    public String   getSecond()           { return second; }
    /** {@return the joined table's soft-delete column, or {@code null} for none} */
    public String   getSoftDeleteColumn() { return softDeleteColumn; }
}
