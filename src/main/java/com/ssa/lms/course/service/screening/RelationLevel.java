package com.ssa.lms.course.service.screening;

/**
 * 선발 편람의 "동일(10) / 관련(9) / 다른(8)" 3단계 — 전공, 경력 및 자격증 항목에 공통으로 쓰인다.
 */
public enum RelationLevel {

    SAME("동일", 10),
    RELATED("관련", 9),
    DIFFERENT("다른", 8);

    private final String label;
    private final int point;

    RelationLevel(String label, int point) {
        this.label = label;
        this.point = point;
    }

    public String getLabel() {
        return label;
    }

    public int getPoint() {
        return point;
    }
}
