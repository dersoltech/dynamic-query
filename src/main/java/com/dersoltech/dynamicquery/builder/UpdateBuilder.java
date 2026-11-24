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
package com.dersoltech.dynamicquery.builder;

import com.dersoltech.dynamicquery.util.SqlFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Builds an UPDATE statement with optional WHERE filtering.
 *
 * <pre>{@code
 * DynamicQuery.update("users")
 *     .set("name", "Alice")
 *     .set("active", false)
 *     .where("id", 42)
 *     .sql();
 * // → UPDATE users SET name = 'Alice', active = FALSE WHERE id = 42
 * }</pre>
 *
 * <p>Calling {@link #sql()} or {@link #parameterizedSql()} without any
 * {@link #set(String, Object)} call throws {@link IllegalStateException}.
 */
public class UpdateBuilder extends WhereCapableBuilder<UpdateBuilder> {

    private final LinkedHashMap<String, Object> assignments = new LinkedHashMap<>();

    /**
     * Creates an UPDATE builder for the given table.
     *
     * @param table the target table name
     * @throws IllegalArgumentException if {@code table} is null or blank
     */
    public UpdateBuilder(String table) {
        super(table);
    }

    // =========================================================================
    // SET
    // =========================================================================

    /**
     * Adds or replaces a {@code column = value} assignment.
     *
     * @param column the column to set
     * @param value  the new value; may be {@code null}
     * @return this builder
     */
    public UpdateBuilder set(String column, Object value) {
        assignments.put(column, value);
        return this;
    }

    // =========================================================================
    // SQL generation
    // =========================================================================

    /**
     * Returns an UPDATE statement with inline literal values.
     *
     * @return the SQL string
     * @throws IllegalStateException if no SET assignment has been added
     */
    public String sql() {
        requireAssignments();
        StringBuilder sb = buildSetClause(new StringBuilder(), null);
        buildWhere(sb, null);
        return sb.toString();
    }

    /**
     * Returns an UPDATE statement with {@code ?} placeholders and the
     * corresponding ordered parameter values.
     *
     * <p>SET parameters appear before WHERE parameters in the list.
     *
     * @return the SQL and its parameters
     * @throws IllegalStateException if no SET assignment has been added
     */
    public ParameterizedQuery parameterizedSql() {
        requireAssignments();
        List<Object> params = new ArrayList<>();
        StringBuilder sb = buildSetClause(new StringBuilder(), params);
        buildWhere(sb, params);
        return new ParameterizedQuery(sb.toString(), Collections.unmodifiableList(params));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private StringBuilder buildSetClause(StringBuilder sb, List<Object> params) {
        sb.append("UPDATE ").append(table).append(" SET ");
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, Object> entry : assignments.entrySet()) {
            if (params != null) {
                joiner.add(entry.getKey() + " = ?");
                params.add(entry.getValue());
            } else {
                joiner.add(entry.getKey() + " = " + SqlFormatter.format(entry.getValue()));
            }
        }
        return sb.append(joiner);
    }

    private void requireAssignments() {
        if (assignments.isEmpty()) {
            throw new IllegalStateException("UPDATE requires at least one SET assignment");
        }
    }
}
