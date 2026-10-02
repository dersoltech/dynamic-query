package com.dersoltech.dynamicquery.builder;

import com.dersoltech.dynamicquery.clauses.HavingClause;
import com.dersoltech.dynamicquery.clauses.JoinClause;
import com.dersoltech.dynamicquery.clauses.OrderByClause;
import com.dersoltech.dynamicquery.clauses.WhereClause;
import com.dersoltech.dynamicquery.enums.JoinType;
import com.dersoltech.dynamicquery.enums.OrderDirection;
import com.dersoltech.dynamicquery.enums.WhereType;
import com.dersoltech.dynamicquery.util.ModelInspector;
import com.dersoltech.dynamicquery.util.SqlFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Fluent SELECT query builder inspired by Laravel Eloquent / Query Builder.
 *
 * <p>All mutating methods return {@code this} so calls can be chained.
 * Call {@link #sql()} at any point to materialize the current state as a
 * SQL string. The builder itself is not thread-safe.
 *
 * <pre>{@code
 * String sql = DynamicQuery.from(User.class)
 *     .select("id", "name", "email")
 *     .where("active", true)
 *     .where("age", ">", 21)
 *     .orderBy("name")
 *     .limit(25)
 *     .sql();
 * }</pre>
 */
public class QueryBuilder extends WhereCapableBuilder<QueryBuilder> {

    private final List<String>        columns  = new ArrayList<>();
    private       boolean             distinct = false;
    private final List<JoinClause>    joins    = new ArrayList<>();
    private final List<String>        groupBys = new ArrayList<>();
    private final List<HavingClause>  havings  = new ArrayList<>();
    private final List<OrderByClause> orders   = new ArrayList<>();
    private       Integer             limitVal  = null;
    private       Integer             offsetVal = null;

    // -------------------------------------------------------------------------
    // Soft-delete state
    // -------------------------------------------------------------------------

    private final String          primarySoftDeleteColumn; // null → model is not soft-deletable
    private       SoftDeleteMode  softDeleteMode = SoftDeleteMode.DEFAULT;

    private enum SoftDeleteMode { DEFAULT, WITH_DELETED, ONLY_DELETED }

    public QueryBuilder(String table) {
        this(table, null);
    }

    public QueryBuilder(String table, String primarySoftDeleteColumn) {
        super(table);
        this.primarySoftDeleteColumn = primarySoftDeleteColumn;
    }

    /**
     * Includes soft-deleted rows in the result — suppresses the automatic
     * {@code IS NULL} filter on the primary table and any soft-deletable
     * JOIN targets.
     */
    public QueryBuilder withDeleted() {
        this.softDeleteMode = SoftDeleteMode.WITH_DELETED;
        return this;
    }

    /**
     * Restricts the result to soft-deleted rows only — changes the primary
     * table filter to {@code IS NOT NULL}. Soft-deletable JOIN targets retain
     * their {@code IS NULL} filter (returning only their active rows).
     */
    public QueryBuilder onlyDeleted() {
        this.softDeleteMode = SoftDeleteMode.ONLY_DELETED;
        return this;
    }

    @Override
    protected List<WhereClause> prependWhereClauses() {
        if (primarySoftDeleteColumn == null || softDeleteMode == SoftDeleteMode.WITH_DELETED) {
            return List.of();
        }
        String qualifiedCol = table + "." + primarySoftDeleteColumn;
        return softDeleteMode == SoftDeleteMode.ONLY_DELETED
                ? List.of(WhereClause.isNotNull(WhereType.AND, qualifiedCol))
                : List.of(WhereClause.isNull(WhereType.AND, qualifiedCol));
    }

    // =========================================================================
    // SELECT
    // =========================================================================

    /**
     * Replaces the column list with the given columns.
     * Passing no arguments selects all columns ({@code SELECT *}).
     */
    public QueryBuilder select(String... cols) {
        columns.clear();
        columns.addAll(Arrays.asList(cols));
        return this;
    }

    /** Appends additional columns without clearing the existing list. */
    public QueryBuilder addSelect(String... cols) {
        columns.addAll(Arrays.asList(cols));
        return this;
    }

    /** Adds a {@code DISTINCT} modifier to the SELECT. */
    public QueryBuilder distinct() {
        this.distinct = true;
        return this;
    }

    // =========================================================================
    // JOIN
    // =========================================================================

    public QueryBuilder join(String joinTable,
                             String first, String operator, String second) {
        joins.add(new JoinClause(JoinType.INNER, joinTable, first, operator, second));
        return this;
    }

    public QueryBuilder leftJoin(String joinTable,
                                 String first, String operator, String second) {
        joins.add(new JoinClause(JoinType.LEFT, joinTable, first, operator, second));
        return this;
    }

    public QueryBuilder rightJoin(String joinTable,
                                  String first, String operator, String second) {
        joins.add(new JoinClause(JoinType.RIGHT, joinTable, first, operator, second));
        return this;
    }

    public QueryBuilder crossJoin(String joinTable) {
        joins.add(new JoinClause(JoinType.CROSS, joinTable, null, null, null));
        return this;
    }

    /**
     * INNER JOIN resolved from a {@code @Model}-annotated class.
     * If the class is also annotated with {@code @SoftDeleted}, the soft-delete
     * filter is added to the ON condition automatically.
     */
    public QueryBuilder join(Class<?> modelClass,
                             String first, String operator, String second) {
        joins.add(joinClauseFrom(JoinType.INNER, modelClass, first, operator, second));
        return this;
    }

    /** LEFT JOIN resolved from a {@code @Model}-annotated class. */
    public QueryBuilder leftJoin(Class<?> modelClass,
                                 String first, String operator, String second) {
        joins.add(joinClauseFrom(JoinType.LEFT, modelClass, first, operator, second));
        return this;
    }

    /** RIGHT JOIN resolved from a {@code @Model}-annotated class. */
    public QueryBuilder rightJoin(Class<?> modelClass,
                                  String first, String operator, String second) {
        joins.add(joinClauseFrom(JoinType.RIGHT, modelClass, first, operator, second));
        return this;
    }

    private static JoinClause joinClauseFrom(JoinType type, Class<?> modelClass,
                                             String first, String operator, String second) {
        String table           = ModelInspector.getTableName(modelClass);
        String softDeleteColumn = ModelInspector.getSoftDeleteColumn(modelClass);
        return new JoinClause(type, table, first, operator, second, softDeleteColumn);
    }

    // =========================================================================
    // GROUP BY / HAVING
    // =========================================================================

    public QueryBuilder groupBy(String... cols) {
        groupBys.addAll(Arrays.asList(cols));
        return this;
    }

    public QueryBuilder having(String column, String operator, Object value) {
        havings.add(HavingClause.basic(WhereType.AND, column, operator, value));
        return this;
    }

    public QueryBuilder orHaving(String column, String operator, Object value) {
        havings.add(HavingClause.basic(WhereType.OR, column, operator, value));
        return this;
    }

    public QueryBuilder havingRaw(String rawSql) {
        havings.add(HavingClause.raw(WhereType.AND, rawSql));
        return this;
    }

    public QueryBuilder orHavingRaw(String rawSql) {
        havings.add(HavingClause.raw(WhereType.OR, rawSql));
        return this;
    }

    // =========================================================================
    // ORDER BY
    // =========================================================================

    /** Adds {@code ORDER BY column ASC}. */
    public QueryBuilder orderBy(String column) {
        orders.add(new OrderByClause(column, OrderDirection.ASC));
        return this;
    }

    /** Adds {@code ORDER BY column direction}. */
    public QueryBuilder orderBy(String column, String direction) {
        orders.add(new OrderByClause(column, OrderDirection.from(direction)));
        return this;
    }

    public QueryBuilder orderByAsc(String column) {
        orders.add(new OrderByClause(column, OrderDirection.ASC));
        return this;
    }

    public QueryBuilder orderByDesc(String column) {
        orders.add(new OrderByClause(column, OrderDirection.DESC));
        return this;
    }

    // =========================================================================
    // LIMIT / OFFSET
    // =========================================================================

    public QueryBuilder limit(int limit) {
        if (limit < 0) throw new IllegalArgumentException("LIMIT must be >= 0");
        this.limitVal = limit;
        return this;
    }

    public QueryBuilder offset(int offset) {
        if (offset < 0) throw new IllegalArgumentException("OFFSET must be >= 0");
        this.offsetVal = offset;
        return this;
    }

    // =========================================================================
    // SQL generation
    // =========================================================================

    /**
     * Builds and returns the SQL string representing the current state of the
     * builder. Each call re-generates the SQL from scratch — the builder is
     * not consumed.
     */
    public String sql() {
        StringBuilder sb = new StringBuilder();
        buildSelect(sb);
        buildFrom(sb);
        buildJoins(sb);
        buildWhere(sb, null);
        buildGroupBy(sb);
        buildHaving(sb, null);
        buildOrderBy(sb);
        buildLimit(sb);
        buildOffset(sb);
        return sb.toString();
    }

    /**
     * Builds a parameterized SQL string and returns it together with the
     * ordered list of parameter values.
     *
     * <p>Every literal value in WHERE and HAVING predicates is replaced with a
     * {@code ?} placeholder. LIMIT and OFFSET are kept as literals because
     * they are not user-supplied data. RAW clauses are emitted unchanged —
     * callers are responsible for any escaping within raw SQL.
     *
     * <p>The {@link ParameterizedQuery#parameters()} list is unmodifiable and
     * ordered left-to-right, matching placeholder positions exactly.
     */
    public ParameterizedQuery parameterizedSql() {
        List<Object> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        buildSelect(sb);
        buildFrom(sb);
        buildJoins(sb);
        buildWhere(sb, params);
        buildGroupBy(sb);
        buildHaving(sb, params);
        buildOrderBy(sb);
        buildLimit(sb);
        buildOffset(sb);
        return new ParameterizedQuery(sb.toString(), Collections.unmodifiableList(params));
    }

    // =========================================================================
    // Private builders
    // =========================================================================

    private void buildSelect(StringBuilder sb) {
        sb.append("SELECT ");
        if (distinct) sb.append("DISTINCT ");
        if (columns.isEmpty()) {
            sb.append('*');
        } else {
            sb.append(String.join(", ", columns));
        }
    }

    private void buildFrom(StringBuilder sb) {
        sb.append(" FROM ").append(table);
    }

    private void buildJoins(StringBuilder sb) {
        for (JoinClause join : joins) {
            sb.append(' ').append(join.getType().getSql())
              .append(' ').append(join.getTable());
            if (join.getFirst() != null) {
                sb.append(" ON ").append(join.getFirst())
                  .append(' ').append(join.getOperator())
                  .append(' ').append(join.getSecond());
                // Soft-delete filter in ON clause (skipped when withDeleted())
                if (join.getSoftDeleteColumn() != null
                        && softDeleteMode != SoftDeleteMode.WITH_DELETED) {
                    sb.append(" AND ").append(join.getTable())
                      .append('.').append(join.getSoftDeleteColumn())
                      .append(" IS NULL");
                }
            }
        }
    }

    private void buildGroupBy(StringBuilder sb) {
        if (groupBys.isEmpty()) return;
        sb.append(" GROUP BY ").append(String.join(", ", groupBys));
    }

    private void buildHaving(StringBuilder sb, List<Object> params) {
        if (havings.isEmpty()) return;

        sb.append(" HAVING ");
        for (int i = 0; i < havings.size(); i++) {
            HavingClause h = havings.get(i);
            if (i > 0) sb.append(' ').append(h.getConnector()).append(' ');
            switch (h.getKind()) {
                case BASIC -> {
                    sb.append(h.getColumn()).append(' ').append(h.getOperator()).append(' ');
                    if (params != null) {
                        sb.append('?');
                        params.add(h.getValue());
                    } else {
                        sb.append(SqlFormatter.format(h.getValue()));
                    }
                }
                case RAW -> sb.append(h.getRawSql());
            }
        }
    }

    private void buildOrderBy(StringBuilder sb) {
        if (orders.isEmpty()) return;
        StringJoiner joiner = new StringJoiner(", ");
        for (OrderByClause o : orders) {
            joiner.add(o.getColumn() + " " + o.getDirection());
        }
        sb.append(" ORDER BY ").append(joiner);
    }

    private void buildLimit(StringBuilder sb) {
        if (limitVal != null) sb.append(" LIMIT ").append(limitVal);
    }

    private void buildOffset(StringBuilder sb) {
        if (offsetVal != null) sb.append(" OFFSET ").append(offsetVal);
    }
}
