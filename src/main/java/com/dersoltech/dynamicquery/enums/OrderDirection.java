package com.dersoltech.dynamicquery.enums;

public enum OrderDirection {
    ASC, DESC;

    public static OrderDirection from(String value) {
        return switch (value.toUpperCase().trim()) {
            case "DESC" -> DESC;
            default -> ASC;
        };
    }
}
