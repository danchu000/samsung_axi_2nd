package com.ssa.lms.exam.service;

/**
 * 응시/제출 도메인 규칙 위반 (기간 외, 횟수 소진, 문항 미확정, 본인인증 미완료 등).
 *
 * 권한 위반(남의 응시 회차 접근)은 이 예외가 아니라
 * {@code org.springframework.security.access.AccessDeniedException} 을 던져 403 으로 나가게 한다.
 */
public class ExamTakeException extends RuntimeException {

    /** 화면이 분기할 수 있게 붙이는 코드. */
    private final String code;

    public ExamTakeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 본인인증이 필요하거나 실패한 경우 — 화면은 인증 모달을 다시 띄운다. */
    public static ExamTakeException identityRequired(String message) {
        return new ExamTakeException("IDENTITY_REQUIRED", message);
    }
}
