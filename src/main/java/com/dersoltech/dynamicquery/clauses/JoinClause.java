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

    /** Constructs a join with no soft-delete awareness. */
    public JoinClause(JoinType type, String table,
                      String first, String operator, String second) {
        this(type, table, first, operator, second, null);
    }

    /** Constructs a join that optionally carries a soft-delete column. */
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

    public JoinType getType()             { return type; }
    public String   getTable()            { return table; }
    public String   getFirst()            { return first; }
    public String   getOperator()         { return operator; }
    public String   getSecond()           { return second; }
    public String   getSoftDeleteColumn() { return softDeleteColumn; }
}
