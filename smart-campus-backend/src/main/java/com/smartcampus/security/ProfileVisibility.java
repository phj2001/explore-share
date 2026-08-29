package com.smartcampus.security;

import java.util.Arrays;

/**
 * 个人主页可见性：控制公开主页及打卡/分享/关注列表/成就等内容端点的可见范围。
 * 与 UserStatus 同风格：DB 列 Short 直存 code，默认 PUBLIC。
 */
public enum ProfileVisibility {

    /** 公开：所有人可见 */
    PUBLIC((short) 0, "PUBLIC"),
    /** 仅关注者：本人与关注者可见 */
    FOLLOWERS((short) 1, "FOLLOWERS"),
    /** 仅自己：仅本人可见 */
    PRIVATE((short) 2, "PRIVATE");

    private final short code;
    private final String name;

    ProfileVisibility(short code, String name) {
        this.code = code;
        this.name = name;
    }

    public short getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ProfileVisibility fromCode(Short code) {
        return Arrays.stream(values())
                .filter(visibility -> visibility.code == (code == null ? PUBLIC.code : code))
                .findFirst()
                .orElse(PUBLIC);
    }

    public static boolean isValidCode(Short code) {
        return Arrays.stream(values()).anyMatch(visibility -> visibility.code == code);
    }
}
