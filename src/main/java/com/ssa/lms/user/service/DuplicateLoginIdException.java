package com.ssa.lms.user.service;

/** 이미 사용 중인 loginId 로 가입을 시도한 경우. */
public class DuplicateLoginIdException extends RuntimeException {
    public DuplicateLoginIdException(String loginId) {
        super("이미 사용 중인 아이디입니다: " + loginId);
    }
}
