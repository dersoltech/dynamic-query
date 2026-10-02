package com.dersoltech.dynamicquery;

import com.dersoltech.dynamicquery.annotations.Column;
import com.dersoltech.dynamicquery.annotations.Model;
import com.dersoltech.dynamicquery.annotations.SoftDeleted;
import com.dersoltech.dynamicquery.builder.ParameterizedQuery;
import com.dersoltech.dynamicquery.util.ModelInspector;
import com.dersoltech.dynamicquery.util.SqlFormatter;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryBuilderTest {

    // -------------------------------------------------------------------------
    // Sample model fixtures
    // -------------------------------------------------------------------------

    @Model(table = "users")
    static class User {
        @Column(name = "user_id") private Long id;
        @Column(name = "first_name") private String firstName;
        @Column private String email;   // uses field name as column name
        private String ignored;         // no @Column — should not appear in mappings
    }

    @Model   // no explicit table → derived from class name
    static class OrderItem {
        @Column(name = "order_item_id") private Long id;
    }

    static class NoAnnotation {}

    // Soft-delete fixtures
    @Model(table = "users")  @SoftDeleted
    static class SoftUser {}

    @Model(table = "orders") @SoftDeleted(column = "removed_at")
    static class SoftOrder {}

    @Model(table = "products") // no @SoftDeleted — used in mixed-join tests
    static class Product {}

    // -------------------------------------------------------------------------
    // ModelInspector tests
    // -------------------------------------------------------------------------

    @Test
    void getTableName_usesExplicitTableAttribute() {
        assertEquals("users", ModelInspector.getTableName(User.class));
    }

    @Test
    void getTableName_derivesFromClassName() {
        assertEquals("order_items", ModelInspector.getTableName(OrderItem.class));
    }

    @Test
    void getTableName_throwsWhenNoAnnotation() {
        assertThrows(IllegalArgumentException.class,
                () -> ModelInspector.getTableName(NoAnnotation.class));
    }

    @Test
    void getColumnMappings_resolvesAnnotatedFields() {
        var mappings = ModelInspector.getColumnMappings(User.class);
        assertEquals("user_id",    mappings.get("id"));
        assertEquals("first_name", mappings.get("firstName"));
        assertEquals("email",      mappings.get("email"));
        assertFalse(mappings.containsKey("ignored"));
    }

    // -------------------------------------------------------------------------
    // DynamicQuery.from / DynamicQuery.table
    // -------------------------------------------------------------------------

    @Test
    void from_resolvesTableFromAnnotation() {
        String sql = DynamicQuery.from(User.class).sql();
        assertTrue(sql.startsWith("SELECT * FROM users"));
    }

    @Test
    void table_usesRawTableName() {
        String sql = DynamicQuery.table("audit_logs").sql();
        assertEquals("SELECT * FROM audit_logs", sql);
    }

    // -------------------------------------------------------------------------
    // SELECT
    // -------------------------------------------------------------------------

    @Test
    void select_specifiedColumns() {
        String sql = DynamicQuery.table("users")
                .select("id", "name", "email")
                .sql();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    @Test
    void select_distinct() {
        String sql = DynamicQuery.table("users")
                .select("country")
                .distinct()
                .sql();
        assertEquals("SELECT DISTINCT country FROM users", sql);
    }

    @Test
    void addSelect_appendsColumns() {
        String sql = DynamicQuery.table("users")
                .select("id", "name")
                .addSelect("email")
                .sql();
        assertEquals("SELECT id, name, email FROM users", sql);
    }

    // -------------------------------------------------------------------------
    // WHERE
    // -------------------------------------------------------------------------

    @Test
    void where_equalityShorthand() {
        String sql = DynamicQuery.table("users")
                .where("active", true)
                .sql();
        assertEquals("SELECT * FROM users WHERE active = TRUE", sql);
    }

    @Test
    void where_explicitOperator() {
        String sql = DynamicQuery.table("users")
                .where("age", ">", 18)
                .sql();
        assertEquals("SELECT * FROM users WHERE age > 18", sql);
    }

    @Test
    void where_stringValue_isSingleQuoted() {
        String sql = DynamicQuery.table("users")
                .where("name", "=", "O'Brien")
                .sql();
        assertEquals("SELECT * FROM users WHERE name = 'O''Brien'", sql);
    }

    @Test
    void where_multipleConditions_joinedWithAnd() {
        String sql = DynamicQuery.table("users")
                .where("active", true)
                .where("age", ">", 18)
                .sql();
        assertEquals("SELECT * FROM users WHERE active = TRUE AND age > 18", sql);
    }

    @Test
    void orWhere_usesOrConnector() {
        String sql = DynamicQuery.table("users")
                .where("role", "admin")
                .orWhere("role", "superuser")
                .sql();
        assertEquals("SELECT * FROM users WHERE role = 'admin' OR role = 'superuser'", sql);
    }

    @Test
    void whereIn_formatsCorrectly() {
        String sql = DynamicQuery.table("users")
                .whereIn("id", List.of(1, 2, 3))
                .sql();
        assertEquals("SELECT * FROM users WHERE id IN (1, 2, 3)", sql);
    }

    @Test
    void whereNotIn_formatsCorrectly() {
        String sql = DynamicQuery.table("users")
                .whereNotIn("status", List.of("banned", "pending"))
                .sql();
        assertEquals("SELECT * FROM users WHERE status NOT IN ('banned', 'pending')", sql);
    }

    @Test
    void whereNull_formatsCorrectly() {
        String sql = DynamicQuery.table("users")
                .whereNull("deleted_at")
                .sql();
        assertEquals("SELECT * FROM users WHERE deleted_at IS NULL", sql);
    }

    @Test
    void whereNotNull_formatsCorrectly() {
        String sql = DynamicQuery.table("users")
                .whereNotNull("email_verified_at")
                .sql();
        assertEquals("SELECT * FROM users WHERE email_verified_at IS NOT NULL", sql);
    }

    @Test
    void whereRaw_embedsLiteralSql() {
        String sql = DynamicQuery.table("users")
                .whereRaw("age BETWEEN 18 AND 65")
                .sql();
        assertEquals("SELECT * FROM users WHERE age BETWEEN 18 AND 65", sql);
    }

    @Test
    void orWhereRaw_usesOrConnector() {
        String sql = DynamicQuery.table("users")
                .where("role", "admin")
                .orWhereRaw("super_user = TRUE")
                .sql();
        assertEquals("SELECT * FROM users WHERE role = 'admin' OR super_user = TRUE", sql);
    }

    // -------------------------------------------------------------------------
    // JOINs
    // -------------------------------------------------------------------------

    @Test
    void join_innerJoin() {
        String sql = DynamicQuery.table("users")
                .select("users.id", "orders.total")
                .join("orders", "users.id", "=", "orders.user_id")
                .sql();
        assertEquals(
                "SELECT users.id, orders.total FROM users INNER JOIN orders ON users.id = orders.user_id",
                sql);
    }

    @Test
    void leftJoin() {
        String sql = DynamicQuery.table("users")
                .leftJoin("profiles", "users.id", "=", "profiles.user_id")
                .sql();
        assertEquals(
                "SELECT * FROM users LEFT JOIN profiles ON users.id = profiles.user_id",
                sql);
    }

    @Test
    void rightJoin() {
        String sql = DynamicQuery.table("orders")
                .rightJoin("users", "orders.user_id", "=", "users.id")
                .sql();
        assertEquals(
                "SELECT * FROM orders RIGHT JOIN users ON orders.user_id = users.id",
                sql);
    }

    @Test
    void crossJoin() {
        String sql = DynamicQuery.table("products")
                .crossJoin("categories")
                .sql();
        assertEquals("SELECT * FROM products CROSS JOIN categories", sql);
    }

    // -------------------------------------------------------------------------
    // GROUP BY / HAVING
    // -------------------------------------------------------------------------

    @Test
    void groupBy_singleColumn() {
        String sql = DynamicQuery.table("orders")
                .select("user_id", "COUNT(*) as total")
                .groupBy("user_id")
                .sql();
        assertEquals("SELECT user_id, COUNT(*) as total FROM orders GROUP BY user_id", sql);
    }

    @Test
    void groupBy_multipleColumns() {
        String sql = DynamicQuery.table("sales")
                .groupBy("year", "month")
                .sql();
        assertEquals("SELECT * FROM sales GROUP BY year, month", sql);
    }

    @Test
    void having_basicCondition() {
        String sql = DynamicQuery.table("orders")
                .select("user_id", "COUNT(*) as cnt")
                .groupBy("user_id")
                .having("COUNT(*)", ">", 5)
                .sql();
        assertEquals(
                "SELECT user_id, COUNT(*) as cnt FROM orders GROUP BY user_id HAVING COUNT(*) > 5",
                sql);
    }

    @Test
    void havingRaw_embedsLiteralSql() {
        String sql = DynamicQuery.table("orders")
                .groupBy("user_id")
                .havingRaw("SUM(amount) > 1000")
                .sql();
        assertEquals("SELECT * FROM orders GROUP BY user_id HAVING SUM(amount) > 1000", sql);
    }

    // -------------------------------------------------------------------------
    // ORDER BY
    // -------------------------------------------------------------------------

    @Test
    void orderBy_defaultsToAsc() {
        String sql = DynamicQuery.table("users")
                .orderBy("name")
                .sql();
        assertEquals("SELECT * FROM users ORDER BY name ASC", sql);
    }

    @Test
    void orderByDesc() {
        String sql = DynamicQuery.table("users")
                .orderByDesc("created_at")
                .sql();
        assertEquals("SELECT * FROM users ORDER BY created_at DESC", sql);
    }

    @Test
    void orderBy_multipleColumns() {
        String sql = DynamicQuery.table("users")
                .orderBy("last_name")
                .orderBy("first_name", "ASC")
                .orderByDesc("created_at")
                .sql();
        assertEquals(
                "SELECT * FROM users ORDER BY last_name ASC, first_name ASC, created_at DESC",
                sql);
    }

    // -------------------------------------------------------------------------
    // LIMIT / OFFSET
    // -------------------------------------------------------------------------

    @Test
    void limit_appendsLimitClause() {
        String sql = DynamicQuery.table("users").limit(10).sql();
        assertEquals("SELECT * FROM users LIMIT 10", sql);
    }

    @Test
    void offset_appendsOffsetClause() {
        String sql = DynamicQuery.table("users").limit(10).offset(20).sql();
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    void limit_negative_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> DynamicQuery.table("users").limit(-1));
    }

    // -------------------------------------------------------------------------
    // Complex / integration-style queries
    // -------------------------------------------------------------------------

    @Test
    void complexQuery_allClauses() {
        String sql = DynamicQuery.from(User.class)
                .select("users.id", "users.first_name", "COUNT(orders.id) as order_count")
                .leftJoin("orders", "users.user_id", "=", "orders.user_id")
                .where("users.active", true)
                .whereNotNull("users.email_verified_at")
                .whereIn("users.role", List.of("customer", "vip"))
                .groupBy("users.id", "users.first_name")
                .having("COUNT(orders.id)", ">=", 1)
                .orderByDesc("order_count")
                .orderBy("users.first_name")
                .limit(25)
                .offset(50)
                .sql();

        assertEquals(
            "SELECT users.id, users.first_name, COUNT(orders.id) as order_count" +
            " FROM users" +
            " LEFT JOIN orders ON users.user_id = orders.user_id" +
            " WHERE users.active = TRUE" +
            " AND users.email_verified_at IS NOT NULL" +
            " AND users.role IN ('customer', 'vip')" +
            " GROUP BY users.id, users.first_name" +
            " HAVING COUNT(orders.id) >= 1" +
            " ORDER BY order_count DESC, users.first_name ASC" +
            " LIMIT 25" +
            " OFFSET 50",
            sql);
    }

    @Test
    void sql_isIdempotent() {
        var builder = DynamicQuery.table("users").where("id", 1).limit(5);
        assertEquals(builder.sql(), builder.sql());
    }

    // -------------------------------------------------------------------------
    // parameterizedSql()
    // -------------------------------------------------------------------------

    @Test
    void parameterizedSql_basicWhere_replacesValueWithPlaceholder() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .where("active", true)
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE active = ?", pq.sql());
        assertEquals(List.of(true), pq.parameters());
    }

    @Test
    void parameterizedSql_stringValue_notQuoted() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .where("name", "=", "O'Brien")
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE name = ?", pq.sql());
        assertEquals(List.of("O'Brien"), pq.parameters());
    }

    @Test
    void parameterizedSql_multipleWheres_parametersInOrder() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .where("active", true)
                .where("age", ">", 21)
                .where("name", "=", "Alice")
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE active = ? AND age > ? AND name = ?", pq.sql());
        assertEquals(List.of(true, 21, "Alice"), pq.parameters());
    }

    @Test
    void parameterizedSql_whereIn_expandsPlaceholders() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .whereIn("role", List.of("admin", "editor"))
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE role IN (?, ?)", pq.sql());
        assertEquals(List.of("admin", "editor"), pq.parameters());
    }

    @Test
    void parameterizedSql_whereNotIn_expandsPlaceholders() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .whereNotIn("status", List.of("banned", "pending"))
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE status NOT IN (?, ?)", pq.sql());
        assertEquals(List.of("banned", "pending"), pq.parameters());
    }

    @Test
    void parameterizedSql_whereNull_producesNoParameter() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .whereNull("deleted_at")
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE deleted_at IS NULL", pq.sql());
        assertTrue(pq.parameters().isEmpty());
    }

    @Test
    void parameterizedSql_whereNotNull_producesNoParameter() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .whereNotNull("email_verified_at")
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE email_verified_at IS NOT NULL", pq.sql());
        assertTrue(pq.parameters().isEmpty());
    }

    @Test
    void parameterizedSql_whereRaw_emittedAsIs_noParameter() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .whereRaw("age BETWEEN 18 AND 65")
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE age BETWEEN 18 AND 65", pq.sql());
        assertTrue(pq.parameters().isEmpty());
    }

    @Test
    void parameterizedSql_orWhere_preservesConnector() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .where("role", "admin")
                .orWhere("role", "superuser")
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE role = ? OR role = ?", pq.sql());
        assertEquals(List.of("admin", "superuser"), pq.parameters());
    }

    @Test
    void parameterizedSql_having_parameterized() {
        ParameterizedQuery pq = DynamicQuery.table("orders")
                .select("user_id", "COUNT(*) as cnt")
                .groupBy("user_id")
                .having("COUNT(*)", ">", 5)
                .parameterizedSql();

        assertEquals(
                "SELECT user_id, COUNT(*) as cnt FROM orders GROUP BY user_id HAVING COUNT(*) > ?",
                pq.sql());
        assertEquals(List.of(5), pq.parameters());
    }

    @Test
    void parameterizedSql_havingRaw_producesNoParameter() {
        ParameterizedQuery pq = DynamicQuery.table("orders")
                .groupBy("user_id")
                .havingRaw("SUM(amount) > 1000")
                .parameterizedSql();

        assertEquals("SELECT * FROM orders GROUP BY user_id HAVING SUM(amount) > 1000", pq.sql());
        assertTrue(pq.parameters().isEmpty());
    }

    @Test
    void parameterizedSql_limitOffset_remainLiteral() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .where("active", true)
                .limit(10)
                .offset(20)
                .parameterizedSql();

        assertEquals("SELECT * FROM users WHERE active = ? LIMIT 10 OFFSET 20", pq.sql());
        assertEquals(List.of(true), pq.parameters());
    }

    @Test
    void parameterizedSql_parametersListIsUnmodifiable() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .where("id", 1)
                .parameterizedSql();

        assertThrows(UnsupportedOperationException.class, () -> pq.parameters().add("hack"));
    }

    @Test
    void parameterizedSql_doesNotAffectSubsequentSelectCall() {
        var builder = DynamicQuery.table("users").where("active", true);
        ParameterizedQuery pq = builder.parameterizedSql();
        String inlineSql = builder.sql();

        assertEquals("SELECT * FROM users WHERE active = ?",    pq.sql());
        assertEquals("SELECT * FROM users WHERE active = TRUE", inlineSql);
    }

    @Test
    void parameterizedSql_complexSelectQuery() {
        ParameterizedQuery pq = DynamicQuery.from(User.class)
                .select("users.id", "users.first_name", "COUNT(orders.id) as order_count")
                .leftJoin("orders", "users.user_id", "=", "orders.user_id")
                .where("users.active", true)
                .whereNotNull("users.email_verified_at")
                .whereIn("users.role", List.of("customer", "vip"))
                .groupBy("users.id", "users.first_name")
                .having("COUNT(orders.id)", ">=", 1)
                .orderByDesc("order_count")
                .orderBy("users.first_name")
                .limit(25)
                .offset(50)
                .parameterizedSql();

        assertEquals(
            "SELECT users.id, users.first_name, COUNT(orders.id) as order_count" +
            " FROM users" +
            " LEFT JOIN orders ON users.user_id = orders.user_id" +
            " WHERE users.active = ?" +
            " AND users.email_verified_at IS NOT NULL" +
            " AND users.role IN (?, ?)" +
            " GROUP BY users.id, users.first_name" +
            " HAVING COUNT(orders.id) >= ?" +
            " ORDER BY order_count DESC, users.first_name ASC" +
            " LIMIT 25" +
            " OFFSET 50",
            pq.sql());
        assertEquals(List.of(true, "customer", "vip", 1), pq.parameters());
    }

    // =========================================================================
    // LIKE
    // =========================================================================

    @Test
    void whereLike_formatsCorrectly() {
        String sql = DynamicQuery.table("users")
                .whereLike("name", "J%")
                .sql();
        assertEquals("SELECT * FROM users WHERE name LIKE 'J%'", sql);
    }

    @Test
    void orWhereLike_formatsCorrectly() {
        String sql = DynamicQuery.table("users")
                .whereLike("name", "J%")
                .orWhereLike("email", "%@example.com")
                .sql();
        assertEquals("SELECT * FROM users WHERE name LIKE 'J%' OR email LIKE '%@example.com'", sql);
    }

    @Test
    void whereNotLike_formatsCorrectly() {
        String sql = DynamicQuery.table("users")
                .where("active", true)
                .whereNotLike("email", "%@spam.com")
                .orWhereNotLike("name", "test_%")
                .sql();
        assertEquals(
                "SELECT * FROM users WHERE active = TRUE AND email NOT LIKE '%@spam.com'" +
                " OR name NOT LIKE 'test_%'",
                sql);
    }

    @Test
    void whereLike_escapesQuotesInPattern() {
        String sql = DynamicQuery.table("users")
                .whereLike("name", "O'B%")
                .sql();
        assertEquals("SELECT * FROM users WHERE name LIKE 'O''B%'", sql);
    }

    @Test
    void parameterizedSql_whereLike_usesPlaceholders() {
        ParameterizedQuery pq = DynamicQuery.table("users")
                .whereLike("name", "J%")
                .orWhereNotLike("email", "%@spam.com")
                .parameterizedSql();
        assertEquals("SELECT * FROM users WHERE name LIKE ? OR email NOT LIKE ?", pq.sql());
        assertEquals(List.of("J%", "%@spam.com"), pq.parameters());
    }

    @Test
    void whereContains_wrapsAndEscapes() {
        String sql = DynamicQuery.table("products")
                .whereContains("code", "50%_off")
                .sql();
        assertEquals("SELECT * FROM products WHERE code LIKE '%50\\%\\_off%' ESCAPE '\\'", sql);
    }

    @Test
    void whereStartsWith_and_whereEndsWith() {
        String sql = DynamicQuery.table("files")
                .whereStartsWith("path", "C:\\tmp")
                .orWhereEndsWith("name", "o'brien.txt")
                .sql();
        assertEquals(
                "SELECT * FROM files WHERE path LIKE 'C:\\\\tmp%' ESCAPE '\\'" +
                " OR name LIKE '%o''brien.txt' ESCAPE '\\'",
                sql);
    }

    @Test
    void orWhereContains_and_orWhereStartsWith_and_whereEndsWith() {
        String sql = DynamicQuery.table("users")
                .whereEndsWith("email", "@example.com")
                .orWhereContains("name", "smith")
                .orWhereStartsWith("name", "Dr.")
                .sql();
        assertEquals(
                "SELECT * FROM users WHERE email LIKE '%@example.com' ESCAPE '\\'" +
                " OR name LIKE '%smith%' ESCAPE '\\'" +
                " OR name LIKE 'Dr.%' ESCAPE '\\'",
                sql);
    }

    @Test
    void parameterizedSql_whereContains_escapesParameterOnly() {
        ParameterizedQuery pq = DynamicQuery.table("products")
                .whereContains("code", "50%")
                .whereStartsWith("sku", "A_")
                .parameterizedSql();
        assertEquals(
                "SELECT * FROM products WHERE code LIKE ? ESCAPE '\\' AND sku LIKE ? ESCAPE '\\'",
                pq.sql());
        assertEquals(List.of("%50\\%%", "A\\_%"), pq.parameters());
    }

    @Test
    void update_withWhereLike() {
        ParameterizedQuery pq = DynamicQuery.update("users")
                .set("active", false)
                .whereLike("email", "%@spam.com")
                .parameterizedSql();
        assertEquals("UPDATE users SET active = ? WHERE email LIKE ?", pq.sql());
        assertEquals(List.of(false, "%@spam.com"), pq.parameters());
    }

    @Test
    void delete_withWhereContains() {
        String sql = DynamicQuery.delete("sessions")
                .whereContains("user_agent", "bot")
                .sql();
        assertEquals("DELETE FROM sessions WHERE user_agent LIKE '%bot%' ESCAPE '\\'", sql);
    }

    @Test
    void escapeLike_escapesWildcardsAndEscapeChar() {
        assertEquals("50\\%\\_off\\\\x", SqlFormatter.escapeLike("50%_off\\x"));
        assertEquals("plain", SqlFormatter.escapeLike("plain"));
    }

    // =========================================================================
    // INSERT
    // =========================================================================

    private static LinkedHashMap<String, Object> row(Object... pairs) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], pairs[i + 1]);
        return map;
    }

    @Test
    void insert_singleRow_inlineSql() {
        String sql = DynamicQuery.insert("users",
                row("name", "Alice", "email", "alice@example.com", "active", true))
                .sql();
        assertEquals(
                "INSERT INTO users (name, email, active) VALUES ('Alice', 'alice@example.com', TRUE)",
                sql);
    }

    @Test
    void insert_singleRow_parameterizedSql() {
        ParameterizedQuery pq = DynamicQuery.insert("users",
                row("name", "Alice", "email", "alice@example.com", "active", true))
                .parameterizedSql();
        assertEquals("INSERT INTO users (name, email, active) VALUES (?, ?, ?)", pq.sql());
        assertEquals(List.of("Alice", "alice@example.com", true), pq.parameters());
    }

    @Test
    void insert_nullValue_rendersNullLiteral() {
        String sql = DynamicQuery.insert("users",
                row("name", "Alice", "deleted_at", null))
                .sql();
        assertEquals("INSERT INTO users (name, deleted_at) VALUES ('Alice', NULL)", sql);
    }

    @Test
    void insertBatch_multipleRows_inlineSql() {
        String sql = DynamicQuery.insertBatch("users", List.of(
                row("name", "Alice", "email", "alice@example.com"),
                row("name", "Bob",   "email", "bob@example.com")))
                .sql();
        assertEquals(
                "INSERT INTO users (name, email) VALUES ('Alice', 'alice@example.com'), ('Bob', 'bob@example.com')",
                sql);
    }

    @Test
    void insertBatch_multipleRows_parameterizedSql() {
        ParameterizedQuery pq = DynamicQuery.insertBatch("users", List.of(
                row("name", "Alice", "email", "alice@example.com"),
                row("name", "Bob",   "email", "bob@example.com")))
                .parameterizedSql();
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?), (?, ?)", pq.sql());
        assertEquals(List.of("Alice", "alice@example.com", "Bob", "bob@example.com"), pq.parameters());
    }

    @Test
    void insert_fromModelClass_resolvesTable() {
        String sql = DynamicQuery.insert(User.class, row("first_name", "Alice"))
                .sql();
        assertTrue(sql.startsWith("INSERT INTO users"));
    }

    @Test
    void insert_noRows_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> DynamicQuery.insertBatch("users", List.of()));
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Test
    void update_singleSet_noWhere() {
        String sql = DynamicQuery.update("users")
                .set("active", false)
                .sql();
        assertEquals("UPDATE users SET active = FALSE", sql);
    }

    @Test
    void update_multipleSet_withWhere() {
        String sql = DynamicQuery.update("users")
                .set("name", "Alice Updated")
                .set("active", true)
                .where("id", 42)
                .sql();
        assertEquals("UPDATE users SET name = 'Alice Updated', active = TRUE WHERE id = 42", sql);
    }

    @Test
    void update_parameterizedSql_setParamsBeforeWhereParams() {
        ParameterizedQuery pq = DynamicQuery.update("users")
                .set("name", "Alice")
                .set("active", false)
                .where("id", 42)
                .parameterizedSql();
        assertEquals("UPDATE users SET name = ?, active = ? WHERE id = ?", pq.sql());
        assertEquals(List.of("Alice", false, 42), pq.parameters());
    }

    @Test
    void update_withWhereIn() {
        String sql = DynamicQuery.update("users")
                .set("active", false)
                .whereIn("role", List.of("banned", "suspended"))
                .sql();
        assertEquals("UPDATE users SET active = FALSE WHERE role IN ('banned', 'suspended')", sql);
    }

    @Test
    void update_fromModelClass_resolvesTable() {
        String sql = DynamicQuery.update(User.class)
                .set("active", false)
                .sql();
        assertTrue(sql.startsWith("UPDATE users"));
    }

    @Test
    void update_noAssignments_throws() {
        assertThrows(IllegalStateException.class,
                () -> DynamicQuery.update("users").where("id", 1).sql());
    }

    @Test
    void update_parameterizedSql_whereInExpandsPlaceholders() {
        ParameterizedQuery pq = DynamicQuery.update("users")
                .set("active", false)
                .whereIn("id", List.of(1, 2, 3))
                .parameterizedSql();
        assertEquals("UPDATE users SET active = ? WHERE id IN (?, ?, ?)", pq.sql());
        assertEquals(List.of(false, 1, 2, 3), pq.parameters());
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Test
    void delete_noWhere_fullTableDelete() {
        String sql = DynamicQuery.delete("sessions").sql();
        assertEquals("DELETE FROM sessions", sql);
    }

    @Test
    void delete_withWhere() {
        String sql = DynamicQuery.delete("users")
                .where("id", 42)
                .sql();
        assertEquals("DELETE FROM users WHERE id = 42", sql);
    }

    @Test
    void delete_multipleConditions() {
        String sql = DynamicQuery.delete("users")
                .where("active", false)
                .whereNull("email_verified_at")
                .sql();
        assertEquals("DELETE FROM users WHERE active = FALSE AND email_verified_at IS NULL", sql);
    }

    @Test
    void delete_parameterizedSql() {
        ParameterizedQuery pq = DynamicQuery.delete("users")
                .where("active", false)
                .whereIn("role", List.of("banned", "guest"))
                .parameterizedSql();
        assertEquals("DELETE FROM users WHERE active = ? AND role IN (?, ?)", pq.sql());
        assertEquals(List.of(false, "banned", "guest"), pq.parameters());
    }

    @Test
    void delete_noWhere_parameterizedSql_emptyParams() {
        ParameterizedQuery pq = DynamicQuery.delete("sessions").parameterizedSql();
        assertEquals("DELETE FROM sessions", pq.sql());
        assertTrue(pq.parameters().isEmpty());
    }

    @Test
    void delete_fromModelClass_resolvesTable() {
        String sql = DynamicQuery.delete(User.class).where("id", 1).sql();
        assertEquals("DELETE FROM users WHERE id = 1", sql);
    }

    // =========================================================================
    // @SoftDeleted — primary table
    // =========================================================================

    @Test
    void softDelete_default_addsIsNullFilter() {
        String sql = DynamicQuery.from(SoftUser.class).sql();
        assertEquals("SELECT * FROM users WHERE users.deleted_at IS NULL", sql);
    }

    @Test
    void softDelete_customColumn_usesAnnotatedName() {
        String sql = DynamicQuery.from(SoftOrder.class).sql();
        assertEquals("SELECT * FROM orders WHERE orders.removed_at IS NULL", sql);
    }

    @Test
    void softDelete_noAnnotation_noFilter() {
        // User has @Model but NOT @SoftDeleted — existing behaviour unchanged
        String sql = DynamicQuery.from(User.class).sql();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    void softDelete_default_prependsBeforeUserWhere() {
        String sql = DynamicQuery.from(SoftUser.class)
                .where("active", true)
                .where("age", ">", 18)
                .sql();
        assertEquals(
                "SELECT * FROM users WHERE users.deleted_at IS NULL AND active = TRUE AND age > 18",
                sql);
    }

    @Test
    void withDeleted_suppressesFilter() {
        String sql = DynamicQuery.from(SoftUser.class)
                .withDeleted()
                .sql();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    void withDeleted_withUserWhere_onlyShowsUserWhere() {
        String sql = DynamicQuery.from(SoftUser.class)
                .withDeleted()
                .where("active", true)
                .sql();
        assertEquals("SELECT * FROM users WHERE active = TRUE", sql);
    }

    @Test
    void onlyDeleted_addsIsNotNullFilter() {
        String sql = DynamicQuery.from(SoftUser.class)
                .onlyDeleted()
                .sql();
        assertEquals("SELECT * FROM users WHERE users.deleted_at IS NOT NULL", sql);
    }

    @Test
    void onlyDeleted_withUserWhere() {
        String sql = DynamicQuery.from(SoftUser.class)
                .onlyDeleted()
                .where("active", false)
                .sql();
        assertEquals(
                "SELECT * FROM users WHERE users.deleted_at IS NOT NULL AND active = FALSE",
                sql);
    }

    @Test
    void softDelete_table_entryPoint_noFilter() {
        // DynamicQuery.table() has no model class → no automatic soft-delete filter
        String sql = DynamicQuery.table("users").sql();
        assertEquals("SELECT * FROM users", sql);
    }

    // =========================================================================
    // @SoftDeleted — JOIN targets
    // =========================================================================

    @Test
    void softDelete_joinWithModelClass_addsOnCondition() {
        String sql = DynamicQuery.from(SoftUser.class)
                .join(SoftOrder.class, "users.id", "=", "orders.user_id")
                .sql();
        assertEquals(
                "SELECT * FROM users" +
                " INNER JOIN orders ON users.id = orders.user_id AND orders.removed_at IS NULL" +
                " WHERE users.deleted_at IS NULL",
                sql);
    }

    @Test
    void softDelete_leftJoinWithModelClass_addsOnCondition() {
        String sql = DynamicQuery.from(SoftUser.class)
                .leftJoin(SoftOrder.class, "users.id", "=", "orders.user_id")
                .sql();
        assertEquals(
                "SELECT * FROM users" +
                " LEFT JOIN orders ON users.id = orders.user_id AND orders.removed_at IS NULL" +
                " WHERE users.deleted_at IS NULL",
                sql);
    }

    @Test
    void softDelete_joinNonSoftDeletedModel_noOnCondition() {
        // Product has no @SoftDeleted → join ON has no extra condition
        String sql = DynamicQuery.from(SoftUser.class)
                .join(Product.class, "users.id", "=", "products.user_id")
                .sql();
        assertEquals(
                "SELECT * FROM users" +
                " INNER JOIN products ON users.id = products.user_id" +
                " WHERE users.deleted_at IS NULL",
                sql);
    }

    @Test
    void withDeleted_suppressesJoinOnConditionToo() {
        String sql = DynamicQuery.from(SoftUser.class)
                .withDeleted()
                .join(SoftOrder.class, "users.id", "=", "orders.user_id")
                .sql();
        assertEquals(
                "SELECT * FROM users" +
                " INNER JOIN orders ON users.id = orders.user_id",
                sql);
    }

    @Test
    void onlyDeleted_joinStillUsesIsNull() {
        // onlyDeleted() flips primary to IS NOT NULL but JOIN target keeps IS NULL
        String sql = DynamicQuery.from(SoftUser.class)
                .onlyDeleted()
                .join(SoftOrder.class, "users.id", "=", "orders.user_id")
                .sql();
        assertEquals(
                "SELECT * FROM users" +
                " INNER JOIN orders ON users.id = orders.user_id AND orders.removed_at IS NULL" +
                " WHERE users.deleted_at IS NOT NULL",
                sql);
    }

    @Test
    void softDelete_stringJoin_noOnCondition() {
        // String-based join overload is not soft-delete-aware
        String sql = DynamicQuery.from(SoftUser.class)
                .join("orders", "users.id", "=", "orders.user_id")
                .sql();
        assertEquals(
                "SELECT * FROM users" +
                " INNER JOIN orders ON users.id = orders.user_id" +
                " WHERE users.deleted_at IS NULL",
                sql);
    }

    @Test
    void softDelete_nonSoftDeletedPrimary_softDeletedJoin() {
        // Primary has no @SoftDeleted; joined table does
        String sql = DynamicQuery.from(User.class)
                .join(SoftOrder.class, "users.user_id", "=", "orders.user_id")
                .sql();
        assertEquals(
                "SELECT * FROM users" +
                " INNER JOIN orders ON users.user_id = orders.user_id AND orders.removed_at IS NULL",
                sql);
    }

    // =========================================================================
    // @SoftDeleted — parameterized SQL
    // =========================================================================

    @Test
    void softDelete_parameterizedSql_noExtraParameters() {
        // IS NULL / IS NOT NULL carry no bind values
        ParameterizedQuery pq = DynamicQuery.from(SoftUser.class)
                .where("active", true)
                .parameterizedSql();
        assertEquals("SELECT * FROM users WHERE users.deleted_at IS NULL AND active = ?", pq.sql());
        assertEquals(List.of(true), pq.parameters());
    }

    @Test
    void softDelete_onlyDeleted_parameterizedSql_noExtraParameters() {
        ParameterizedQuery pq = DynamicQuery.from(SoftUser.class)
                .onlyDeleted()
                .where("role", "admin")
                .parameterizedSql();
        assertEquals(
                "SELECT * FROM users WHERE users.deleted_at IS NOT NULL AND role = ?",
                pq.sql());
        assertEquals(List.of("admin"), pq.parameters());
    }

    @Test
    void softDelete_joinOnCondition_parameterizedSql_noExtraParameters() {
        ParameterizedQuery pq = DynamicQuery.from(SoftUser.class)
                .join(SoftOrder.class, "users.id", "=", "orders.user_id")
                .where("users.active", true)
                .parameterizedSql();
        assertEquals(
                "SELECT * FROM users" +
                " INNER JOIN orders ON users.id = orders.user_id AND orders.removed_at IS NULL" +
                " WHERE users.deleted_at IS NULL AND users.active = ?",
                pq.sql());
        assertEquals(List.of(true), pq.parameters());
    }
}
