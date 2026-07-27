package com.ssa.lms.course.entity;

public enum CourseStatus {
    DRAFT("작성중"),
    RECRUITING("모집중"),
    IN_PROGRESS("진행중"),
    COMPLETED("종료"),
    CLOSED("폐강");

    private final String label;

    CourseStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
