package com.ssa.lms.course.web;

import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;

import java.time.LocalDateTime;

/** 관리자 과정 상세 화면용 수강신청 뷰 (신청자 명단). */
public record EnrollmentView(Long id, Long traineeId, String traineeName,
                             EnrollmentStatus status, String statusLabel, LocalDateTime appliedAt) {
    public static EnrollmentView of(Enrollment e) {
        return new EnrollmentView(e.getId(), e.getTrainee().getId(), e.getTrainee().getName(),
                e.getStatus(), e.getStatus().getLabel(), e.getAppliedAt());
    }
}
