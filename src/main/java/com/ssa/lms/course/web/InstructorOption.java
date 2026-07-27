package com.ssa.lms.course.web;

/** 강사 배정 드롭다운 옵션 (role=INSTRUCTOR 사용자). */
public record InstructorOption(Long userId, String name) {
}
