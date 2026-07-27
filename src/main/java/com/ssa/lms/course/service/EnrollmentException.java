package com.ssa.lms.course.service;

/** 수강신청 업무 규칙 위반(모집중 아님/중복 신청/정원 초과/권한 없음 등). */
public class EnrollmentException extends RuntimeException {
    public EnrollmentException(String message) {
        super(message);
    }
}
