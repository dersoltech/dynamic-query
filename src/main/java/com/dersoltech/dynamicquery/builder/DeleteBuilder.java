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
