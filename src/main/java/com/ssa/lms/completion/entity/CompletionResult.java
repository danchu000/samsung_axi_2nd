package com.ssa.lms.completion.entity;

/** 이수 자동 판정 결과(진도+출결+성적 기준). */
public enum CompletionResult {
    PENDING("판정대기"),
    PASS("이수"),
    FAIL("미이수");

    private final String label;

    CompletionResult(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
