package com.ssa.lms.grading.service;

/**
 * 채점 규칙 위반. 컨트롤러가 코드별로 400/409 를 골라 내려준다.
 *
 * <p>권한 위반은 이 예외가 아니라 {@code AccessDeniedException} 을 쓴다 — 403 이어야
 * 화면과 감사 로그가 "규칙 위반"과 "권한 없음"을 구분할 수 있다.</p>
 */
public class GradingException extends RuntimeException {

    private final String code;

    public GradingException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 확정된 성적을 사유 없이 바꾸려 한 경우. 내역서 증빙 요건이라 절대 통과시키지 않는다. */
    public static GradingException reasonRequired() {
        return new GradingException("REASON_REQUIRED",
                "확정된 성적입니다. 변경 사유를 입력해야 점수를 수정할 수 있습니다.");
    }
}
