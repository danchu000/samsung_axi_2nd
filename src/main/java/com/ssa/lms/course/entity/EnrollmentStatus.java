package com.ssa.lms.course.entity;

public enum EnrollmentStatus {
    APPLIED("신청"),
    APPROVED("승인"),
    REJECTED("반려"),
    CANCELLED("취소"),
    COMPLETED("수료");

    private final String label;

    EnrollmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
