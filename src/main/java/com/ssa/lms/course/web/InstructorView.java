package com.ssa.lms.course.web;

import com.ssa.lms.course.entity.CourseInstructor;

/** 과정 상세 화면용 담당 강사 매핑 뷰 (mappingId = course_instructor.id). */
public record InstructorView(Long mappingId, Long userId, String name, boolean primary) {
    public static InstructorView of(CourseInstructor ci) {
        return new InstructorView(ci.getId(), ci.getInstructor().getId(),
                ci.getInstructor().getName(), ci.isPrimaryInstructor());
    }
}
