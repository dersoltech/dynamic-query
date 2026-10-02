package com.dersoltech.dynamicquery.clauses;

import com.dersoltech.dynamicquery.enums.WhereType;

/**
 * Represents a single predicate in a HAVING clause.
 */
public final class HavingClause {

    public enum Kind { BASIC, RAW }

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

    public static HavingClause basic(WhereType connector,
                                     String column, String operator, Object value) {
        return new HavingClause(connector, Kind.BASIC, column, operator, value, null);
    }

    public static HavingClause raw(WhereType connector, String rawSql) {
        return new HavingClause(connector, Kind.RAW, null, null, null, rawSql);
    }

    public WhereType getConnector() { return connector; }
    public Kind      getKind()      { return kind; }
    public String    getColumn()    { return column; }
    public String    getOperator()  { return operator; }
    public Object    getValue()     { return value; }
    public String    getRawSql()    { return rawSql; }
}
