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
package com.dersoltech.dynamicquery.util;

import com.dersoltech.dynamicquery.annotations.Column;
import com.dersoltech.dynamicquery.annotations.Model;
import com.dersoltech.dynamicquery.annotations.SoftDeleted;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reflects on a class annotated with {@link Model} to extract table and column metadata.
 */
public final class ModelInspector {

    private ModelInspector() {}

    /**
     * Returns the database table name for the given model class.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>The {@code table} attribute of the {@link Model} annotation if non-blank.</li>
     *   <li>Otherwise the simple class name converted to plural snake_case
     *       (e.g. {@code UserProfile} → {@code user_profiles}).</li>
     * </ol>
     *
     * @param modelClass the model class
     * @return the table name
     * @throws IllegalArgumentException if the class is not annotated with {@link Model}
     */
    public static String getTableName(Class<?> modelClass) {
        Model annotation = modelClass.getAnnotation(Model.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    modelClass.getName() + " is not annotated with @Model");
        }
        String explicit = annotation.table().trim();
        return explicit.isEmpty() ? deriveTableName(modelClass.getSimpleName()) : explicit;
    }

    /**
     * Returns a mapping of Java field name → database column name for every field
     * in the class hierarchy that is annotated with {@link Column}.
     *
     * @param modelClass the model class
     * @return field name to column name mappings, in declaration order
     */
    public static Map<String, String> getColumnMappings(Class<?> modelClass) {
        Map<String, String> mappings = new LinkedHashMap<>();
        Class<?> cursor = modelClass;
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                Column col = field.getAnnotation(Column.class);
                if (col != null) {
                    String colName = col.name().trim();
                    mappings.put(field.getName(),
                            colName.isEmpty() ? field.getName() : colName);
                }
            }
            cursor = cursor.getSuperclass();
        }
        return mappings;
    }

    /**
     * Returns the soft-delete column name if the class is annotated with
     * {@link SoftDeleted}, or {@code null} if it is not soft-deletable.
     *
     * @param modelClass the model class
     * @return the soft-delete column name, or {@code null}
     */
    public static String getSoftDeleteColumn(Class<?> modelClass) {
        SoftDeleted annotation = modelClass.getAnnotation(SoftDeleted.class);
        if (annotation == null) return null;
        String col = annotation.column().trim();
        return col.isEmpty() ? "deleted_at" : col;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Converts {@code CamelCase} → {@code camel_cases} (plural snake_case). */
    static String deriveTableName(String simpleName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        String snake = sb.toString();
        // naive pluralisation: append 's' unless already ends in 's'
        return snake.endsWith("s") ? snake : snake + "s";
    }
}
