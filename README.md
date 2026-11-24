# DynamicQuery

A small, dependency-free Java library for building SQL **at runtime** with a fluent API.

DynamicQuery only *generates* SQL (and bind parameters). It never opens a connection, so database
access, connection pooling, and transaction management stay entirely in your application, using
whatever you already have: plain JDBC, Spring `JdbcTemplate`, HikariCP, etc.

## Why?

JPA's Criteria API and jOOQ are excellent when the shape of a query is known at compile time. They
are awkward when it is not, for example when:

- the columns, filters, and sort orders come from user input, configuration, or another system;
- a single query mixes columns from tables of different "type roots" (joined tables that don't share
  one entity or generated record type);
- you don't want generated metamodels or code generation just to build an ad hoc query.

DynamicQuery treats tables and columns as plain strings, so you can compose queries freely and get
back a SQL string plus an ordered parameter list.

## Features

- **Fluent builders** for `SELECT`, `INSERT` (single and multi-row), `UPDATE`, and `DELETE`
- **Rich `WHERE` API**: `where`, `orWhere`, `whereIn`, `whereNotIn`, `whereNull`, `whereNotNull`,
  `whereLike`, `whereNotLike`, and raw fragments via `whereRaw`
- **Safe LIKE helpers**: `whereContains`, `whereStartsWith`, `whereEndsWith` escape `%`, `_`, and `\`
  in the supplied value and add an `ESCAPE` clause
- **Joins**: `INNER`, `LEFT`, `RIGHT`, and `CROSS`, by table name or by model class
- **`GROUP BY` / `HAVING`**, `DISTINCT`, `ORDER BY`, `LIMIT`, and `OFFSET`
- **Two output modes**: `sql()` for inlined literals, `parameterizedSql()` for `?` placeholders plus an
  ordered, unmodifiable parameter list
- **Optional model annotations** (`@Model`, `@Column`, `@SoftDeleted`) to resolve table names from
  classes
- **Automatic soft-delete filtering** for `@SoftDeleted` models, including joined tables, with
  `withDeleted()` and `onlyDeleted()` overrides
- **No runtime dependencies**, no code generation, no connection handling
- Requires **Java 21+**

## Installation

The project is not yet published to a public repository. Build and install it locally:

```bash
mvn clean install              # snapshot build (default): 1.0-SNAPSHOT
mvn clean install -Prelease    # release build: 1.0, plus sources and javadoc jars
```

The base version is the `revision` property in `pom.xml`; the active profile
(`snapshot` by default, or `release`) decides whether `-SNAPSHOT` is appended.

Then add it to your project:

```xml
<dependency>
    <groupId>com.dersoltech</groupId>
    <artifactId>dynamic-query</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage

Everything starts from the static `DynamicQuery` class.

### SELECT

```java
import com.dersoltech.dynamicquery.DynamicQuery;

String sql = DynamicQuery.table("users")
        .select("id", "name", "email")
        .where("active", true)
        .where("age", ">", 21)
        .orderBy("name")
        .limit(25)
        .sql();
// SELECT id, name, email FROM users WHERE active = TRUE AND age > 21 ORDER BY name ASC LIMIT 25
```

### Parameterized queries (recommended)

Use `parameterizedSql()` whenever values come from outside your code. Values become `?` placeholders
and are returned in order, ready for a `PreparedStatement`.

```java
ParameterizedQuery pq = DynamicQuery.table("users")
        .where("active", true)
        .whereIn("role", List.of("admin", "editor"))
        .whereContains("name", "smith")
        .parameterizedSql();

pq.sql();         // SELECT * FROM users WHERE active = ? AND role IN (?, ?) AND name LIKE ? ESCAPE '\'
pq.parameters();  // [true, "admin", "editor", "%smith%"]
```

Executing it with plain JDBC (you own the connection and pool):

```java
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(pq.sql())) {

    List<Object> params = pq.parameters();
    for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
    }
    try (ResultSet rs = ps.executeQuery()) {
        // map rows...
    }
}
```

### Building queries dynamically

Because every call just appends to the builder, conditional logic is trivial, including joins across
unrelated tables:

```java
QueryBuilder q = DynamicQuery.table("users")
        .select("users.id", "users.first_name", "COUNT(orders.id) AS order_count")
        .leftJoin("orders", "users.id", "=", "orders.user_id");

if (filters.onlyActive()) {
    q.where("users.active", true);
}
if (!filters.roles().isEmpty()) {
    q.whereIn("users.role", filters.roles());
}
if (filters.search() != null) {
    q.whereContains("users.first_name", filters.search());
}

ParameterizedQuery pq = q
        .groupBy("users.id", "users.first_name")
        .having("COUNT(orders.id)", ">=", 1)
        .orderByDesc("order_count")
        .limit(25)
        .offset(50)
        .parameterizedSql();
```

### INSERT, UPDATE, DELETE

```java
// Single row (column order follows the map's iteration order)
Map<String, Object> row = new LinkedHashMap<>();
row.put("name", "Alice");
row.put("email", "alice@example.com");
DynamicQuery.insert("users", row).parameterizedSql();
// INSERT INTO users (name, email) VALUES (?, ?)

// Multi-row
DynamicQuery.insertBatch("users", List.of(rowA, rowB)).parameterizedSql();
// INSERT INTO users (name, email) VALUES (?, ?), (?, ?)

// UPDATE (SET parameters come before WHERE parameters)
DynamicQuery.update("users")
        .set("active", false)
        .where("id", 42)
        .parameterizedSql();
// UPDATE users SET active = ? WHERE id = ?

// DELETE
DynamicQuery.delete("sessions")
        .whereContains("user_agent", "bot")
        .sql();
// DELETE FROM sessions WHERE user_agent LIKE '%bot%' ESCAPE '\'
```

### Model annotations (optional)

Annotate a class to resolve table names from it instead of passing strings.

```java
@Model(table = "users")        // table defaults to snake_case plural of the class name if omitted
@SoftDeleted                   // soft-delete column defaults to deleted_at
class User {
    @Column(name = "user_id") private Long id;
    @Column private String email;
}

@Model(table = "orders")
@SoftDeleted(column = "removed_at")
class Order {}

DynamicQuery.from(User.class).where("active", true).sql();
// SELECT * FROM users WHERE users.deleted_at IS NULL AND active = TRUE

DynamicQuery.from(User.class)
        .join(Order.class, "users.user_id", "=", "orders.user_id")
        .sql();
// SELECT * FROM users
//   INNER JOIN orders ON users.user_id = orders.user_id AND orders.removed_at IS NULL
//   WHERE users.deleted_at IS NULL

DynamicQuery.from(User.class).withDeleted().sql();   // include soft-deleted rows
DynamicQuery.from(User.class).onlyDeleted().sql();   // only soft-deleted rows
```

Soft-delete filtering applies to models resolved from classes. `DynamicQuery.table("...")` and
string-based joins never add it.

## Things to know

- **Identifiers are not escaped.** Table names, column names, operators, and raw fragments
  (`whereRaw`, `havingRaw`, `select(...)`) are written into the SQL as given. Never pass untrusted
  input as an identifier or raw fragment; validate or allow-list it first.
- **Prefer `parameterizedSql()`.** `sql()` inlines literals (escaping single quotes) and is intended
  for logging, debugging, and tests. `LIMIT` and `OFFSET` are always emitted as literals.
- **Builders are not thread-safe**, but `sql()` / `parameterizedSql()` can be called repeatedly and do
  not consume the builder.
- `update(...)` without any `set(...)` throws `IllegalStateException`; `insertBatch` with no rows
  throws `IllegalArgumentException`.
- `update` and `delete` without a `WHERE` clause affect every row, so add conditions deliberately.
- SQL dialect differences (for example `LIMIT`/`OFFSET` syntax) are not abstracted; the output targets
  databases that accept standard `LIMIT ... OFFSET ...`.

## Building

```bash
mvn test      # run the unit tests
mvn verify    # also checks license headers on source files
```

## License

Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt).
