package com.dersoltech.dynamicquery.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a field to a database column.
 *
 * <p>When {@code name} is omitted the field name itself is used as-is.
 *
 * <pre>{@code
 * @Column(name = "first_name")
 * private String firstName;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    /**
     * The column name in the database table.
     * Defaults to the annotated field's name when blank.
     */
    String name() default "";
}
