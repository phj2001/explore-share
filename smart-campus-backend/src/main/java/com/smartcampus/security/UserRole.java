package com.smartcampus.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;

public enum UserRole {

    USER((short) 1, "USER", "ROLE_USER"),
    SUPER_ADMIN((short) 2, "SUPER_ADMIN", "ROLE_SUPER_ADMIN"),
    ADMIN((short) 3, "ADMIN", "ROLE_ADMIN");

    private final short code;
    private final String roleName;
    private final String authority;

    UserRole(short code, String roleName, String authority) {
        this.code = code;
        this.roleName = roleName;
        this.authority = authority;
    }

    public short getCode() {
        return code;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getAuthority() {
        return authority;
    }

    public SimpleGrantedAuthority toAuthority() {
        return new SimpleGrantedAuthority(authority);
    }

    public static UserRole fromCode(Short code) {
        if (code == null) {
            return USER;
        }
        return Arrays.stream(values())
                .filter(role -> role.code == code)
                .findFirst()
                .orElse(USER);
    }
}
