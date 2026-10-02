package com.dersoltech.dynamicquery.builder;

import java.util.List;

/**
 * The result of {@link QueryBuilder#parameterizedSql()}: a SQL string with
 * {@code ?} placeholders and the corresponding ordered parameter values.
 *
 * <p>Parameters appear in the same left-to-right order as their placeholders,
 * making the list directly usable with JDBC {@code PreparedStatement} or any
 * framework that accepts positional parameters.
 *
 * <pre>{@code
 * ParameterizedQuery pq = DynamicQuery.from(User.class)
 *         .where("active", true)
 *         .whereIn("role", List.of("admin", "editor"))
 *         .limit(10)
 *         .parameterizedSql();
 *
 * // pq.sql()        → "SELECT * FROM users WHERE active = ? AND role IN (?, ?) LIMIT 10"
 * // pq.parameters() → [true, "admin", "editor"]
 * }</pre>
 */
public record ParameterizedQuery(String sql, List<Object> parameters) {}
