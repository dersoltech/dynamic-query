package com.dersoltech.dynamicquery;

import com.dersoltech.dynamicquery.builder.DeleteBuilder;
import com.dersoltech.dynamicquery.builder.InsertBuilder;
import com.dersoltech.dynamicquery.builder.QueryBuilder;
import com.dersoltech.dynamicquery.builder.UpdateBuilder;
import com.dersoltech.dynamicquery.util.ModelInspector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Static entry point for all statement builders.
 *
 * <pre>{@code
 * // SELECT
 * DynamicQuery.from(User.class).select("id", "name").where("active", true).sql();
 * DynamicQuery.table("audit_logs").whereNotNull("resolved_at").orderByDesc("created_at").sql();
 *
 * // INSERT
 * DynamicQuery.insert("users", row).sql();
 * DynamicQuery.insertBatch("users", rows).parameterizedSql();
 *
 * // UPDATE
 * DynamicQuery.update("users").set("active", false).where("id", 42).sql();
 *
 * // DELETE
 * DynamicQuery.delete("users").where("id", 42).sql();
 * }</pre>
 */
public final class DynamicQuery {

    private DynamicQuery() {}

    // =========================================================================
    // SELECT
    // =========================================================================

    /**
     * Creates a {@link QueryBuilder} targeting the table defined by the
     * {@code @Model} annotation on the given class.
     *
     * <p>If the class is also annotated with {@code @SoftDeleted}, the builder
     * automatically adds a soft-delete filter to every generated SELECT.
     *
     * @throws IllegalArgumentException if the class lacks a {@code @Model} annotation
     */
    public static QueryBuilder from(Class<?> modelClass) {
        return new QueryBuilder(
                ModelInspector.getTableName(modelClass),
                ModelInspector.getSoftDeleteColumn(modelClass));
    }

    /** Creates a {@link QueryBuilder} targeting an explicit table name. */
    public static QueryBuilder table(String tableName) {
        return new QueryBuilder(tableName);
    }

    // =========================================================================
    // INSERT
    // =========================================================================

    /** Builds an INSERT for a single row into the given table. */
    public static InsertBuilder insert(String tableName, Map<String, Object> row) {
        return new InsertBuilder(tableName, List.of(new LinkedHashMap<>(row)));
    }

    /** Builds an INSERT for a single row, resolving the table from {@code @Model}. */
    public static InsertBuilder insert(Class<?> modelClass, Map<String, Object> row) {
        return insert(ModelInspector.getTableName(modelClass), row);
    }

    /** Builds a multi-row INSERT into the given table. */
    public static InsertBuilder insertBatch(String tableName,
                                            List<? extends Map<String, Object>> rows) {
        List<LinkedHashMap<String, Object>> linked = rows.stream()
                .map(LinkedHashMap::new)
                .collect(Collectors.toList());
        return new InsertBuilder(tableName, linked);
    }

    /** Builds a multi-row INSERT, resolving the table from {@code @Model}. */
    public static InsertBuilder insertBatch(Class<?> modelClass,
                                            List<? extends Map<String, Object>> rows) {
        return insertBatch(ModelInspector.getTableName(modelClass), rows);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /** Creates an {@link UpdateBuilder} targeting the given table. */
    public static UpdateBuilder update(String tableName) {
        return new UpdateBuilder(tableName);
    }

    /** Creates an {@link UpdateBuilder}, resolving the table from {@code @Model}. */
    public static UpdateBuilder update(Class<?> modelClass) {
        return new UpdateBuilder(ModelInspector.getTableName(modelClass));
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /** Creates a {@link DeleteBuilder} targeting the given table. */
    public static DeleteBuilder delete(String tableName) {
        return new DeleteBuilder(tableName);
    }

    /** Creates a {@link DeleteBuilder}, resolving the table from {@code @Model}. */
    public static DeleteBuilder delete(Class<?> modelClass) {
        return new DeleteBuilder(ModelInspector.getTableName(modelClass));
    }
}
