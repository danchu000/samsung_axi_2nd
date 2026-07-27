package com.ssa.lms.support.dto;

import com.ssa.lms.course.entity.Course;

/**
 * 과정 셀렉트 박스 옵션 — trainee/tutoring.html 의 "학습 범위 선택".
 * 원본 화면은 static/js/trainee/tutoring.js 의 mockCourses 배열을 썼다.
 */
public record CourseOption(Long id, String name) {

    public static CourseOption of(Course c) {
        return new CourseOption(c.getId(), c.getCourseName());
    }
}
