package com.ssa.lms.course.web;

import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 훈련생 "나의 과정" 화면용 수강 이력 뷰. */
public record MyEnrollmentView(Long enrollmentId, Long courseId, String courseName, String cohort,
                               String category, EnrollmentStatus status, String statusLabel,
                               LocalDate startDate, LocalDate endDate,
                               double progressRate, LocalDateTime appliedAt) {
    public static MyEnrollmentView of(Enrollment e) {
        var c = e.getCourse();
        return new MyEnrollmentView(e.getId(), c.getId(), c.getCourseName(), c.getCohort(),
                c.getCategory(), e.getStatus(), e.getStatus().getLabel(),
                c.getStartDate(), c.getEndDate(), e.getProgressRate(), e.getAppliedAt());
    }
}
