package com.ssa.lms.user.entity;

/** 권한 3종 — Spring Security ROLE_ 접두어와 함께 사용 */
public enum Role {
    ADMIN("관리자"),
    INSTRUCTOR("강사"),
    TRAINEE("훈련생");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** ex) ROLE_ADMIN */
    public String authority() {
        return "ROLE_" + name();
    }
}
