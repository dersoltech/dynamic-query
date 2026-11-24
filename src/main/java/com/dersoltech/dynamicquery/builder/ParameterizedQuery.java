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
 *
 * @param sql        the SQL string with {@code ?} placeholders
 * @param parameters the parameter values, in placeholder order
 */
public record ParameterizedQuery(String sql, List<Object> parameters) {}
