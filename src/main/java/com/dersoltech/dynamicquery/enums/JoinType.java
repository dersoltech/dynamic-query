package com.dersoltech.dynamicquery.enums;

public enum JoinType {
    INNER("INNER JOIN"),
    LEFT("LEFT JOIN"),
    RIGHT("RIGHT JOIN"),
    CROSS("CROSS JOIN");

    private final String sql;

    JoinType(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
