package com.ssa.lms.course.service;

/** 이미 존재하는 과정 코드로 등록 시. */
public class DuplicateCourseCodeException extends RuntimeException {
    public DuplicateCourseCodeException(String courseCode) {
        super("이미 사용 중인 과정 코드입니다: " + courseCode);
    }
}
