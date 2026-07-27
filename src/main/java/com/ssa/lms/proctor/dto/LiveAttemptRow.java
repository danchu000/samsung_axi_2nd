package com.ssa.lms.proctor.dto;

/**
 * 실시간 감독 화면의 응시자 한 명.
 *
 * <p>카메라 스트리밍은 이 슬라이스 범위 밖이다 (화면의 비디오는 여전히 샘플 영상 더미).
 * 서버가 책임지는 것은 <b>누가 응시 중이고, 남은 시간이 얼마고, 이상행위가 몇 건인지</b>다.</p>
 *
 * @param remainSeconds 서버가 {@code ExamAttempt.expiresAt} 으로 계산한 잔여 초. 만료/제출이면 0.
 * @param warnCount     Severity=WARN 이벤트 수
 * @param criticalCount Severity=CRITICAL 이벤트 수 — 화면에서 빨간 표시
 * @param warningSent   감독관이 실제로 발송한 경고 수 (ProctorWarning)
 */
public record LiveAttemptRow(
        Long attemptId,
        Long userId,
        String traineeName,
        String loginId,
        int attemptNo,
        String status,
        String statusLabel,
        String startedAt,
        String expiresAt,
        String submittedAt,
        long remainSeconds,
        String remainLabel,
        long eventCount,
        long warnCount,
        long criticalCount,
        long warningSent,
        String ip,
        boolean live
) {
}
