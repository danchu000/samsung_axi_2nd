package com.ssa.lms.course.web;

import com.ssa.lms.course.entity.Enrollment;

import java.time.LocalDateTime;

/** 관리자 통합 수강신청 승인 화면용 뷰 — 과정 정보까지 포함 (과정 상세용 {@link EnrollmentView} 와 구분). */
public record PendingEnrollmentView(Long id, Long courseId, String courseCode, String courseName,
                                    String traineeName, String traineeLoginId, LocalDateTime appliedAt) {
    public static PendingEnrollmentView of(Enrollment e) {
        return new PendingEnrollmentView(e.getId(), e.getCourse().getId(),
                e.getCourse().getCourseCode(), e.getCourse().getCourseName(),
                e.getTrainee().getName(), e.getTrainee().getLoginId(), e.getAppliedAt());
    }
}
