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

    protected final String           table;
    protected final List<WhereClause> wheres = new ArrayList<>();

    protected WhereCapableBuilder(String table) {
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("Table name must not be blank");
        }
        this.table = table;
    }

    @SuppressWarnings("unchecked")
    protected final T self() { return (T) this; }

    // =========================================================================
    // WHERE
    // =========================================================================

    /** {@code AND column = value} */
    public T where(String column, Object value) {
        return where(column, "=", value);
    }

    /** {@code AND column operator value} */
    public T where(String column, String operator, Object value) {
        wheres.add(WhereClause.basic(WhereType.AND, column, operator, value));
        return self();
    }

    /** {@code OR column = value} */
    public T orWhere(String column, Object value) {
        return orWhere(column, "=", value);
    }

    /** {@code OR column operator value} */
    public T orWhere(String column, String operator, Object value) {
        wheres.add(WhereClause.basic(WhereType.OR, column, operator, value));
        return self();
    }

    /** {@code AND column IN (values)} */
    public T whereIn(String column, Collection<?> values) {
        wheres.add(WhereClause.in(WhereType.AND, column, toObjectList(values)));
        return self();
    }

    /** {@code OR column IN (values)} */
    public T orWhereIn(String column, Collection<?> values) {
        wheres.add(WhereClause.in(WhereType.OR, column, toObjectList(values)));
        return self();
    }

    /** {@code AND column NOT IN (values)} */
    public T whereNotIn(String column, Collection<?> values) {
        wheres.add(WhereClause.notIn(WhereType.AND, column, toObjectList(values)));
        return self();
    }

    /** {@code OR column NOT IN (values)} */
    public T orWhereNotIn(String column, Collection<?> values) {
        wheres.add(WhereClause.notIn(WhereType.OR, column, toObjectList(values)));
        return self();
    }

    /** {@code AND column IS NULL} */
    public T whereNull(String column) {
        wheres.add(WhereClause.isNull(WhereType.AND, column));
        return self();
    }

    /** {@code OR column IS NULL} */
    public T orWhereNull(String column) {
        wheres.add(WhereClause.isNull(WhereType.OR, column));
        return self();
    }

    /** {@code AND column IS NOT NULL} */
    public T whereNotNull(String column) {
        wheres.add(WhereClause.isNotNull(WhereType.AND, column));
        return self();
    }

    /** {@code OR column IS NOT NULL} */
    public T orWhereNotNull(String column) {
        wheres.add(WhereClause.isNotNull(WhereType.OR, column));
        return self();
    }

    /** {@code AND column LIKE pattern} — caller supplies wildcards. */
    public T whereLike(String column, String pattern) {
        wheres.add(WhereClause.like(WhereType.AND, column, pattern, null));
        return self();
    }

    /** {@code OR column LIKE pattern} — caller supplies wildcards. */
    public T orWhereLike(String column, String pattern) {
        wheres.add(WhereClause.like(WhereType.OR, column, pattern, null));
        return self();
    }

    /** {@code AND column NOT LIKE pattern} — caller supplies wildcards. */
    public T whereNotLike(String column, String pattern) {
        wheres.add(WhereClause.notLike(WhereType.AND, column, pattern, null));
        return self();
    }

    /** {@code OR column NOT LIKE pattern} — caller supplies wildcards. */
    public T orWhereNotLike(String column, String pattern) {
        wheres.add(WhereClause.notLike(WhereType.OR, column, pattern, null));
        return self();
    }

    /** {@code AND column LIKE '%value%'} — wildcards in {@code value} match literally. */
    public T whereContains(String column, String value) {
        return escapedLike(WhereType.AND, column, "%" + SqlFormatter.escapeLike(value) + "%");
    }

    /** {@code OR column LIKE '%value%'} — wildcards in {@code value} match literally. */
    public T orWhereContains(String column, String value) {
        return escapedLike(WhereType.OR, column, "%" + SqlFormatter.escapeLike(value) + "%");
    }

    /** {@code AND column LIKE 'value%'} — wildcards in {@code value} match literally. */
    public T whereStartsWith(String column, String value) {
        return escapedLike(WhereType.AND, column, SqlFormatter.escapeLike(value) + "%");
    }

    /** {@code OR column LIKE 'value%'} — wildcards in {@code value} match literally. */
    public T orWhereStartsWith(String column, String value) {
        return escapedLike(WhereType.OR, column, SqlFormatter.escapeLike(value) + "%");
    }

    /** {@code AND column LIKE '%value'} — wildcards in {@code value} match literally. */
    public T whereEndsWith(String column, String value) {
        return escapedLike(WhereType.AND, column, "%" + SqlFormatter.escapeLike(value));
    }

    /** {@code OR column LIKE '%value'} — wildcards in {@code value} match literally. */
    public T orWhereEndsWith(String column, String value) {
        return escapedLike(WhereType.OR, column, "%" + SqlFormatter.escapeLike(value));
    }

    private T escapedLike(WhereType connector, String column, String pattern) {
        wheres.add(WhereClause.like(connector, column, pattern, SqlFormatter.LIKE_ESCAPE));
        return self();
    }

    /** {@code AND <rawSql>} — caller is responsible for correct SQL. */
    public T whereRaw(String rawSql) {
        wheres.add(WhereClause.raw(WhereType.AND, rawSql));
        return self();
    }

    /** {@code OR <rawSql>} — caller is responsible for correct SQL. */
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

    @SuppressWarnings("unchecked")
    protected static List<Object> toObjectList(Collection<?> values) {
        return new ArrayList<>(values);
    }
}
