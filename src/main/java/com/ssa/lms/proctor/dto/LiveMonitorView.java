package com.ssa.lms.proctor.dto;

import java.util.List;

/**
 * 실시간 감독 화면 전체 모델.
 *
 * @param canVoid 응시 무효 처리 버튼 노출 여부. 권한정의서(1) 16행 — 관리자만 true.
 *                (화면에서 감추는 것과 별개로 서버가 다시 검사한다)
 */
public record LiveMonitorView(
        Long examId,
        String examName,
        String courseName,
        String courseId,
        String instructorName,
        String windowStart,
        String windowEnd,
        int timeLimitMin,
        boolean proctorEnabled,
        boolean requireWebcam,
        int inProgress,
        int completed,
        int notStarted,
        int voided,
        int enrolled,
        boolean canVoid,
        /** 액션 URL 접두어. 화면이 {@code attemptUrlPrefix + attemptId + '/warning'} 처럼 조립한다. */
        String attemptUrlPrefix,
        /** 목록 화면으로 돌아가는 경로 (관리자/강사가 다르다). */
        String listUrl,
        List<LiveAttemptRow> rows
) {
}
