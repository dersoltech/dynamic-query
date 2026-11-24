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
package com.dersoltech.dynamicquery.enums;

/**
 * The kinds of SQL JOIN supported by the query builder.
 */
public enum JoinType {
    /** {@code INNER JOIN} — only rows with a match in both tables. */
    INNER("INNER JOIN"),
    /** {@code LEFT JOIN} — all rows from the left table, matched rows from the right. */
    LEFT("LEFT JOIN"),
    /** {@code RIGHT JOIN} — all rows from the right table, matched rows from the left. */
    RIGHT("RIGHT JOIN"),
    /** {@code CROSS JOIN} — the Cartesian product of both tables. */
    CROSS("CROSS JOIN");

    private final String sql;

    JoinType(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the SQL keyword(s) for this join type.
     *
     * @return the SQL keyword(s), e.g. {@code "LEFT JOIN"}
     */
    public String getSql() {
        return sql;
    }
}
