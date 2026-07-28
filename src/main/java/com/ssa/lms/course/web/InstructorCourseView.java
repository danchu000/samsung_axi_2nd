package com.ssa.lms.course.web;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;

import java.time.LocalDate;

/**
 * 강사 "담당 과정" 목록 카드용 뷰 (읽기 전용).
 * OSIV 비활성 → 서비스 트랜잭션 내에서 과목 수·수강생 수를 미리 집계해 담는다.
 */
public record InstructorCourseView(Long id, String courseCode, String courseName, String cohort,
                                   String category, CourseStatus status, String statusLabel,
                                   LocalDate startDate, LocalDate endDate, int capacity,
                                   long subjectCount, long traineeCount) {

    public static InstructorCourseView of(Course c, long subjectCount, long traineeCount) {
        return new InstructorCourseView(c.getId(), c.getCourseCode(), c.getCourseName(), c.getCohort(),
                c.getCategory(), c.getStatus(), c.getStatus().getLabel(),
                c.getStartDate(), c.getEndDate(), c.getCapacity(), subjectCount, traineeCount);
    }
}
