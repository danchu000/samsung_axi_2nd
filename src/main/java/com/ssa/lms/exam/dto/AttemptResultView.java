package com.ssa.lms.exam.dto;

/**
 * 제출 직후 응시자에게 보여줄 결과 요약.
 *
 * 자동 채점 결과만 담는다. 수동 채점(서술형)과 Grade 반영은 다음 슬라이스(시험 채점) 담당이라
 * 여기서는 {@code manualPending} 으로 "채점 대기"만 표시한다.
 */
public record AttemptResultView(
        String attemptId,
        String examName,
        /** SUBMITTED | AUTO_SUBMITTED */
        String status,
        /** 만료로 서버가 자동 제출했는지 — 응시자에게 반드시 알려야 한다. */
        boolean autoSubmitted,
        String submittedAt,
        Integer autoScore,
        Integer totalScore,
        int examTotalScore,
        int passScore,
        Boolean passed,
        /** 수동 채점 대기 문항이 있으면 true — 이 경우 합격 여부는 아직 확정이 아니다. */
        boolean manualPending,
        int answeredCount,
        int questionCount
) {
}
