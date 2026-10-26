package com.onedao.auth.model;

import java.util.Arrays;

/** Allowed user types: "Standard User" and "Administrator". */
public enum UserType {
    STANDARD_USER("Standard User", "USER"),
    ADMINISTRATOR("Administrator", "ADMIN");

    private final String label;
    private final String role;   // Spring Security role name (without ROLE_ prefix)

    UserType(String label, String role) {
        this.label = label;
        this.role = role;
    }

    public String getLabel() { return label; }
    public String getRole() { return role; }

    public static UserType fromLabel(String label) {
        return Arrays.stream(values())
                .filter(t -> t.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid user type: " + label));
    }
}
