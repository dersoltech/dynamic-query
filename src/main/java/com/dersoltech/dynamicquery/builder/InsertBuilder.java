package com.dersoltech.dynamicquery.builder;

import com.dersoltech.dynamicquery.util.SqlFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringJoiner;

/**
 * Builds an INSERT statement for one or more rows.
 *
 * <p>Column order follows the insertion order of the maps provided at
 * construction time. Use {@link LinkedHashMap} (or
 * {@link java.util.SequencedMap}) when column ordering matters.
 *
 * <pre>{@code
 * // Single row
 * DynamicQuery.insert("users", row).sql();
 * // → INSERT INTO users (name, email) VALUES ('Alice', 'alice@example.com')
 *
 * // Batch
 * DynamicQuery.insertBatch("users", List.of(row1, row2)).sql();
 * // → INSERT INTO users (name, email) VALUES ('Alice', 'a@b.com'), ('Bob', 'b@b.com')
 * }</pre>
 */
public class InsertBuilder {

    private final String                             table;
    private final List<LinkedHashMap<String, Object>> rows;

    public InsertBuilder(String table, List<LinkedHashMap<String, Object>> rows) {
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("Table name must not be blank");
        }
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("INSERT requires at least one row");
        }
        this.table = table;
        this.rows  = List.copyOf(rows);
    }

    // =========================================================================
    // SQL generation
    // =========================================================================

    /** Returns an INSERT statement with inline literal values. */
    public String sql() {
        List<String> cols = columnNames();
        StringBuilder sb = buildPrefix(cols);

        StringJoiner rowJoiner = new StringJoiner(", ");
        for (LinkedHashMap<String, Object> row : rows) {
            StringJoiner valueJoiner = new StringJoiner(", ", "(", ")");
            for (String col : cols) {
                valueJoiner.add(SqlFormatter.format(row.get(col)));
            }
            rowJoiner.add(valueJoiner.toString());
        }
        return sb.append(rowJoiner).toString();
    }

    /**
     * Returns an INSERT statement with {@code ?} placeholders and the
     * corresponding ordered parameter values.
     *
     * <p>Parameters are ordered row-by-row, column-by-column.
     */
    public ParameterizedQuery parameterizedSql() {
        List<String> cols   = columnNames();
        List<Object> params = new ArrayList<>();
        StringBuilder sb    = buildPrefix(cols);

        StringJoiner rowJoiner = new StringJoiner(", ");
        for (LinkedHashMap<String, Object> row : rows) {
            rowJoiner.add(SqlFormatter.placeholders(cols.size()));
            for (String col : cols) {
                params.add(row.get(col));
            }
        }
        sb.append(rowJoiner);
        return new ParameterizedQuery(sb.toString(), Collections.unmodifiableList(params));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<String> columnNames() {
        return new ArrayList<>(rows.getFirst().keySet());
    }

    private StringBuilder buildPrefix(List<String> cols) {
        return new StringBuilder("INSERT INTO ")
                .append(table)
                .append(" (").append(String.join(", ", cols)).append(")")
                .append(" VALUES ");
    }
}
