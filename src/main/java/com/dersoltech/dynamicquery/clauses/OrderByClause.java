package com.dersoltech.dynamicquery.clauses;

import com.dersoltech.dynamicquery.enums.OrderDirection;

/**
 * Represents a single column in an ORDER BY clause.
 */
public final class OrderByClause {

    private final String column;
    private final OrderDirection direction;

    public OrderByClause(String column, OrderDirection direction) {
        this.column    = column;
        this.direction = direction;
    }

    public String         getColumn()    { return column; }
    public OrderDirection getDirection() { return direction; }
}
