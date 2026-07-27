package com.ssa.lms.auth;

/**
 * 본인인증 요청 (a-requests.md P1-7).
 *
 * @param method     인증 수단 코드 — 현재 "PASSWORD" 만 지원, 외부 본인인증(PASS/SMS) 연동 시 확장
 * @param credential 수단별 자격 값 (PASSWORD 면 평문 비밀번호)
 */
public record VerifyRequest(String method, String credential) {

    public static final String METHOD_PASSWORD = "PASSWORD";

    public static VerifyRequest password(String rawPassword) {
        return new VerifyRequest(METHOD_PASSWORD, rawPassword);
    }
}
