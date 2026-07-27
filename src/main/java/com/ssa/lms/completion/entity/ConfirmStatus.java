package com.ssa.lms.completion.entity;

/**
 * 이수 확정 상태(관리자 확정 흐름). 자동 판정({@link CompletionResult}) 후 관리자가 최종 확정한다.
 * 화면(admin-attendance-graduate)의 빠른 상태변경 옵션과 대응: 이수예정/판정대기/확정.
 */
public enum ConfirmStatus {
    EXPECTED("이수예정"),
    PENDING("판정대기"),
    CONFIRMED("확정");

    private final String label;

    ConfirmStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
