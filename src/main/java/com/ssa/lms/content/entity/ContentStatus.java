package com.ssa.lms.content.entity;

/** 콘텐츠 노출 상태 — 프론트 콘텐츠 목록의 활성화/비활성화 뱃지(Active/Archived)에 대응. */
public enum ContentStatus {

    ACTIVE("활성화"),
    ARCHIVED("비활성화");

    private final String label;

    ContentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
