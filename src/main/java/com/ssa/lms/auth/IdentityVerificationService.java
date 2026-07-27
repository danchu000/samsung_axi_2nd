package com.ssa.lms.auth;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 본인인증 계약 (a-requests.md P1-7) — 내역서 필수 요건:
 * 로그인 시 본인인증과 평가(진행단계/최종/과제) 응시 입장 시 재인증이 같은 모듈을 쓴다.
 *
 * <p>B 사용처: 시험 입장 시 {@code verify(...)} 결과 수단 코드와
 * {@code lastVerifiedAt(...)} 을 {@code ExamAttempt.identityVerifiedAt / identityVerifyMethod} 에 저장.</p>
 */
public interface IdentityVerificationService {

    /**
     * 인증 수행. 성공 시 사용한 수단 코드를 반환 (ex. "PASSWORD", "SMS", "PASS").
     * 실패 시 {@link IdentityVerificationException} 발생.
     */
    String verify(Long userId, VerifyRequest request);

    /** 최근 인증 성공 시각. 시험 입장 시 유효기간(예: 30분 이내 재인증 면제) 판정에 쓴다. */
    Optional<LocalDateTime> lastVerifiedAt(Long userId);
}
