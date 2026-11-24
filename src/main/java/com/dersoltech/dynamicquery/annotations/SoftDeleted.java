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
package com.dersoltech.dynamicquery.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a model as soft-deletable.
 *
 * <p>When a class annotated with both {@link Model} and {@link SoftDeleted} is
 * used as the primary table via {@code DynamicQuery.from(...)}, the generated
 * SELECT automatically appends a {@code WHERE table.column IS NULL} predicate
 * to exclude logically-deleted rows.
 *
 * <p>The filter can be overridden per query:
 * <ul>
 *   <li>{@code .withDeleted()} — include all rows regardless of deletion status</li>
 *   <li>{@code .onlyDeleted()} — restrict to rows where the column is NOT NULL</li>
 * </ul>
 *
 * <p>When a soft-deletable model is used as a JOIN target via
 * {@code join(Model.class, ...)}, the soft-delete check is added to the
 * JOIN's ON condition rather than the WHERE clause.
 *
 * <pre>{@code
 * @Model(table = "users")
 * @SoftDeleted                         // uses default column "deleted_at"
 * public class User { ... }
 *
 * @Model(table = "posts")
 * @SoftDeleted(column = "archived_at") // custom column
 * public class Post { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SoftDeleted {
    /**
     * The column that stores the soft-delete timestamp.
     * A {@code NULL} value means the row is active; non-{@code NULL} means deleted.
     * Defaults to {@code "deleted_at"}.
     *
     * @return the soft-delete column name
     */
    String column() default "deleted_at";
}
