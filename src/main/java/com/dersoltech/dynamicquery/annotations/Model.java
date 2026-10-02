package com.dersoltech.dynamicquery.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a database model and maps it to a table.
 *
 * <p>If {@code table} is omitted, the table name is derived by converting the
 * simple class name to snake_case and appending an "s" suffix — e.g.
 * {@code UserProfile} → {@code user_profiles}.
 *
 * <pre>{@code
 * @Model(table = "users")
 * public class User { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Model {
    /**
     * The database table name this model maps to.
     * Defaults to a pluralized snake_case of the class name when blank.
     */
    String table() default "";
}
