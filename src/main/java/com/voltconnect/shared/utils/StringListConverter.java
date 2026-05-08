package com.voltconnect.shared.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JPA {@link AttributeConverter} that maps a {@code List<String>} in Java
 * to a PostgreSQL {@code text[]} column and back.
 *
 * <p>The converter serialises the list as a PostgreSQL array literal
 * (e.g. {@code {"driver","host"}}) so it can be stored in a {@code text[]}
 * column without requiring a Hibernate dialect extension.
 *
 * <p>Used by {@link com.voltconnect.auth.UserEntity} for the
 * {@code role}, {@code vehicle_type}, and {@code connector_type} columns.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        // Build PostgreSQL array literal: {"val1","val2"}
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < attribute.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            // Escape double-quotes inside values
            sb.append("\"").append(attribute.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || dbData.equals("{}")) {
            return new ArrayList<>();
        }
        // Strip surrounding braces
        String inner = dbData.trim();
        if (inner.startsWith("{")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith("}")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        if (inner.isBlank()) {
            return new ArrayList<>();
        }
        // Split on commas that are not inside quotes
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else if (c == '\\' && i + 1 < inner.length() && inner.charAt(i + 1) == '"') {
                current.append('"');
                i++; // skip escaped quote
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }
}
