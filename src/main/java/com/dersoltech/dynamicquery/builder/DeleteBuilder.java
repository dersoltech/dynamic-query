package com.dersoltech.dynamicquery.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a DELETE statement with optional WHERE filtering.
 *
 * <pre>{@code
 * DynamicQuery.delete("users")
 *     .where("active", false)
 *     .whereNull("email_verified_at")
 *     .sql();
 * // → DELETE FROM users WHERE active = FALSE AND email_verified_at IS NULL
 * }</pre>
 *
 * <p>Omitting a WHERE clause generates an unconditional {@code DELETE FROM table},
 * which deletes every row — use with caution.
 */
public class DeleteBuilder extends WhereCapableBuilder<DeleteBuilder> {

    public DeleteBuilder(String table) {
        super(table);
    }

    // =========================================================================
    // SQL generation
    // =========================================================================

    /** Returns a DELETE statement with inline literal values. */
    public String sql() {
        StringBuilder sb = new StringBuilder("DELETE FROM ").append(table);
        buildWhere(sb, null);
        return sb.toString();
    }

    /**
     * Returns a DELETE statement with {@code ?} placeholders and the
     * corresponding ordered parameter values.
     */
    public ParameterizedQuery parameterizedSql() {
        List<Object> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder("DELETE FROM ").append(table);
        buildWhere(sb, params);
        return new ParameterizedQuery(sb.toString(), Collections.unmodifiableList(params));
    }
}
