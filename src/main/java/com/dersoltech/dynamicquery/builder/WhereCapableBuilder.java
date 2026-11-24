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

import com.dersoltech.dynamicquery.clauses.WhereClause;
import com.dersoltech.dynamicquery.enums.WhereType;
import com.dersoltech.dynamicquery.util.SqlFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base class for any builder that can carry a WHERE clause.
 *
 * <p>Subclasses receive the full WHERE fluent API and a protected
 * {@link #buildWhere(StringBuilder, List)} method.  The type parameter
 * {@code T} is the concrete subclass, enabling method chaining to return the
 * correct type throughout the hierarchy.
 *
 * @param <T> the concrete builder type
 */
public abstract class WhereCapableBuilder<T extends WhereCapableBuilder<T>> {

    /** The target table name. */
    protected final String           table;
    /** User-supplied WHERE predicates, in the order they were added. */
    protected final List<WhereClause> wheres = new ArrayList<>();

    /**
     * Creates a builder targeting the given table.
     *
     * @param table the target table name
     * @throws IllegalArgumentException if {@code table} is null or blank
     */
    protected WhereCapableBuilder(String table) {
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("Table name must not be blank");
        }
        this.table = table;
    }

    /**
     * Returns this builder cast to its concrete type, for fluent chaining.
     *
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    protected final T self() { return (T) this; }

    // =========================================================================
    // WHERE
    // =========================================================================

    /**
     * {@code AND column = value}
     *
     * @param column the column name
     * @param value the value to compare against
     * @return this builder
     */
    public T where(String column, Object value) {
        return where(column, "=", value);
    }

    /**
     * {@code AND column operator value}
     *
     * @param column the column name
     * @param operator the comparison operator, e.g. {@code >}
     * @param value the value to compare against
     * @return this builder
     */
    public T where(String column, String operator, Object value) {
        wheres.add(WhereClause.basic(WhereType.AND, column, operator, value));
        return self();
    }

    /**
     * {@code OR column = value}
     *
     * @param column the column name
     * @param value the value to compare against
     * @return this builder
     */
    public T orWhere(String column, Object value) {
        return orWhere(column, "=", value);
    }

    /**
     * {@code OR column operator value}
     *
     * @param column the column name
     * @param operator the comparison operator, e.g. {@code >}
     * @param value the value to compare against
     * @return this builder
     */
    public T orWhere(String column, String operator, Object value) {
        wheres.add(WhereClause.basic(WhereType.OR, column, operator, value));
        return self();
    }

    /**
     * {@code AND column IN (values)}
     *
     * @param column the column name
     * @param values the values to match
     * @return this builder
     */
    public T whereIn(String column, Collection<?> values) {
        wheres.add(WhereClause.in(WhereType.AND, column, toObjectList(values)));
        return self();
    }

    /**
     * {@code OR column IN (values)}
     *
     * @param column the column name
     * @param values the values to match
     * @return this builder
     */
    public T orWhereIn(String column, Collection<?> values) {
        wheres.add(WhereClause.in(WhereType.OR, column, toObjectList(values)));
        return self();
    }

    /**
     * {@code AND column NOT IN (values)}
     *
     * @param column the column name
     * @param values the values to exclude
     * @return this builder
     */
    public T whereNotIn(String column, Collection<?> values) {
        wheres.add(WhereClause.notIn(WhereType.AND, column, toObjectList(values)));
        return self();
    }

    /**
     * {@code OR column NOT IN (values)}
     *
     * @param column the column name
     * @param values the values to exclude
     * @return this builder
     */
    public T orWhereNotIn(String column, Collection<?> values) {
        wheres.add(WhereClause.notIn(WhereType.OR, column, toObjectList(values)));
        return self();
    }

    /**
     * {@code AND column IS NULL}
     *
     * @param column the column name
     * @return this builder
     */
    public T whereNull(String column) {
        wheres.add(WhereClause.isNull(WhereType.AND, column));
        return self();
    }

    /**
     * {@code OR column IS NULL}
     *
     * @param column the column name
     * @return this builder
     */
    public T orWhereNull(String column) {
        wheres.add(WhereClause.isNull(WhereType.OR, column));
        return self();
    }

    /**
     * {@code AND column IS NOT NULL}
     *
     * @param column the column name
     * @return this builder
     */
    public T whereNotNull(String column) {
        wheres.add(WhereClause.isNotNull(WhereType.AND, column));
        return self();
    }

    /**
     * {@code OR column IS NOT NULL}
     *
     * @param column the column name
     * @return this builder
     */
    public T orWhereNotNull(String column) {
        wheres.add(WhereClause.isNotNull(WhereType.OR, column));
        return self();
    }

    /**
     * {@code AND column LIKE pattern} — caller supplies wildcards.
     *
     * @param column the column name
     * @param pattern the LIKE pattern, including any wildcards
     * @return this builder
     */
    public T whereLike(String column, String pattern) {
        wheres.add(WhereClause.like(WhereType.AND, column, pattern, null));
        return self();
    }

    /**
     * {@code OR column LIKE pattern} — caller supplies wildcards.
     *
     * @param column the column name
     * @param pattern the LIKE pattern, including any wildcards
     * @return this builder
     */
    public T orWhereLike(String column, String pattern) {
        wheres.add(WhereClause.like(WhereType.OR, column, pattern, null));
        return self();
    }

    /**
     * {@code AND column NOT LIKE pattern} — caller supplies wildcards.
     *
     * @param column the column name
     * @param pattern the LIKE pattern, including any wildcards
     * @return this builder
     */
    public T whereNotLike(String column, String pattern) {
        wheres.add(WhereClause.notLike(WhereType.AND, column, pattern, null));
        return self();
    }

    /**
     * {@code OR column NOT LIKE pattern} — caller supplies wildcards.
     *
     * @param column the column name
     * @param pattern the LIKE pattern, including any wildcards
     * @return this builder
     */
    public T orWhereNotLike(String column, String pattern) {
        wheres.add(WhereClause.notLike(WhereType.OR, column, pattern, null));
        return self();
    }

    /**
     * {@code AND column LIKE '%value%'} — wildcards in {@code value} match literally.
     *
     * @param column the column name
     * @param value the text to match literally
     * @return this builder
     */
    public T whereContains(String column, String value) {
        return escapedLike(WhereType.AND, column, "%" + SqlFormatter.escapeLike(value) + "%");
    }

    /**
     * {@code OR column LIKE '%value%'} — wildcards in {@code value} match literally.
     *
     * @param column the column name
     * @param value the text to match literally
     * @return this builder
     */
    public T orWhereContains(String column, String value) {
        return escapedLike(WhereType.OR, column, "%" + SqlFormatter.escapeLike(value) + "%");
    }

    /**
     * {@code AND column LIKE 'value%'} — wildcards in {@code value} match literally.
     *
     * @param column the column name
     * @param value the text to match literally
     * @return this builder
     */
    public T whereStartsWith(String column, String value) {
        return escapedLike(WhereType.AND, column, SqlFormatter.escapeLike(value) + "%");
    }

    /**
     * {@code OR column LIKE 'value%'} — wildcards in {@code value} match literally.
     *
     * @param column the column name
     * @param value the text to match literally
     * @return this builder
     */
    public T orWhereStartsWith(String column, String value) {
        return escapedLike(WhereType.OR, column, SqlFormatter.escapeLike(value) + "%");
    }

    /**
     * {@code AND column LIKE '%value'} — wildcards in {@code value} match literally.
     *
     * @param column the column name
     * @param value the text to match literally
     * @return this builder
     */
    public T whereEndsWith(String column, String value) {
        return escapedLike(WhereType.AND, column, "%" + SqlFormatter.escapeLike(value));
    }

    /**
     * {@code OR column LIKE '%value'} — wildcards in {@code value} match literally.
     *
     * @param column the column name
     * @param value the text to match literally
     * @return this builder
     */
    public T orWhereEndsWith(String column, String value) {
        return escapedLike(WhereType.OR, column, "%" + SqlFormatter.escapeLike(value));
    }

    private T escapedLike(WhereType connector, String column, String pattern) {
        wheres.add(WhereClause.like(connector, column, pattern, SqlFormatter.LIKE_ESCAPE));
        return self();
    }

    /**
     * {@code AND <rawSql>} — caller is responsible for correct SQL.
     *
     * @param rawSql the SQL fragment, inserted verbatim
     * @return this builder
     */
    public T whereRaw(String rawSql) {
        wheres.add(WhereClause.raw(WhereType.AND, rawSql));
        return self();
    }

    /**
     * {@code OR <rawSql>} — caller is responsible for correct SQL.
     *
     * @param rawSql the SQL fragment, inserted verbatim
     * @return this builder
     */
    public T orWhereRaw(String rawSql) {
        wheres.add(WhereClause.raw(WhereType.OR, rawSql));
        return self();
    }

    // =========================================================================
    // Protected builder helpers
    // =========================================================================

    /**
     * Returns clauses to prepend before user-supplied WHERE predicates.
     *
     * <p>The default implementation returns an empty list. Subclasses
     * (e.g. {@code QueryBuilder} for soft-delete filtering) may override this
     * to inject automatic conditions without touching the user-visible
     * {@code wheres} list.
     *
     * @return the clauses to emit before the user-supplied predicates
     */
    protected List<WhereClause> prependWhereClauses() {
        return List.of();
    }

    /**
     * Appends a WHERE clause to {@code sb}.
     *
     * <p>Prepended clauses (from {@link #prependWhereClauses()}) are emitted
     * first, followed by user-added predicates.
     *
     * @param sb     the SQL being built
     * @param params if non-null, values are replaced with {@code ?} and
     *               appended to this list; if null, values are inlined.
     */
    protected void buildWhere(StringBuilder sb, List<Object> params) {
        List<WhereClause> prepended = prependWhereClauses();
        if (prepended.isEmpty() && wheres.isEmpty()) return;

        sb.append(" WHERE ");
        int i = 0;
        for (WhereClause w : prepended) {
            if (i > 0) sb.append(' ').append(w.getConnector()).append(' ');
            appendWhereClause(sb, w, params);
            i++;
        }
        for (WhereClause w : wheres) {
            if (i > 0) sb.append(' ').append(w.getConnector()).append(' ');
            appendWhereClause(sb, w, params);
            i++;
        }
    }

    private static void appendWhereClause(StringBuilder sb, WhereClause w, List<Object> params) {
        switch (w.getKind()) {
            case BASIC -> {
                sb.append(w.getColumn()).append(' ').append(w.getOperator()).append(' ');
                if (params != null) {
                    sb.append('?');
                    params.add(w.getValue());
                } else {
                    sb.append(SqlFormatter.format(w.getValue()));
                }
            }
            case IN -> {
                sb.append(w.getColumn()).append(" IN ");
                if (params != null) {
                    sb.append(SqlFormatter.placeholders(w.getValues().size()));
                    params.addAll(w.getValues());
                } else {
                    sb.append(SqlFormatter.formatList(w.getValues()));
                }
            }
            case NOT_IN -> {
                sb.append(w.getColumn()).append(" NOT IN ");
                if (params != null) {
                    sb.append(SqlFormatter.placeholders(w.getValues().size()));
                    params.addAll(w.getValues());
                } else {
                    sb.append(SqlFormatter.formatList(w.getValues()));
                }
            }
            case LIKE, NOT_LIKE -> {
                sb.append(w.getColumn())
                  .append(w.getKind() == WhereClause.Kind.LIKE ? " LIKE " : " NOT LIKE ");
                if (params != null) {
                    sb.append('?');
                    params.add(w.getValue());
                } else {
                    sb.append(SqlFormatter.format(w.getValue()));
                }
                if (w.getEscape() != null) {
                    sb.append(" ESCAPE ").append(SqlFormatter.format(String.valueOf(w.getEscape())));
                }
            }
            case NULL     -> sb.append(w.getColumn()).append(" IS NULL");
            case NOT_NULL -> sb.append(w.getColumn()).append(" IS NOT NULL");
            case RAW      -> sb.append(w.getRawSql());
        }
    }

    /**
     * Copies a collection of arbitrary element type into a mutable {@code List<Object>}.
     *
     * @param values the values to copy
     * @return a new list containing {@code values}
     */
    protected static List<Object> toObjectList(Collection<?> values) {
        return new ArrayList<>(values);
    }
}
