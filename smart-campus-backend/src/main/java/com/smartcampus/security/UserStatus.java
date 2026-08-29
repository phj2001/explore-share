package com.smartcampus.security;

import java.util.Arrays;

public enum UserStatus {

    DISABLED((short) 0, "DISABLED"),
    ACTIVE((short) 1, "ACTIVE"),
    /** 已注销：用户自主注销账号后的终态（匿名化保留内容），不可登录、不可恢复 */
    CANCELLED((short) 2, "CANCELLED");

    private final short code;
    private final String name;

    UserStatus(short code, String name) {
        this.code = code;
        this.name = name;
    }

    public short getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static UserStatus fromCode(Short code) {
        return Arrays.stream(values())
                .filter(status -> status.code == (code == null ? ACTIVE.code : code))
                .findFirst()
                .orElse(ACTIVE);
    }

    public static boolean isValidCode(Short code) {
        return Arrays.stream(values()).anyMatch(status -> status.code == code);
    }
}
