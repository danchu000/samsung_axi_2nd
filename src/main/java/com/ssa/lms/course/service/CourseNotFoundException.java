package com.ssa.lms.course.service;

/** 존재하지 않거나 이미 삭제된 과정 조회 시. */
public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Long id) {
        super("과정을 찾을 수 없습니다: " + id);
    }
}
