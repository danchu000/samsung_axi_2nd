package com.ssa.lms.grading.dto;

/**
 * 응시자별 채점 현황 한 행 — {@code result-grading.html} 의 {@code #studentTableBody}.
 *
 * <p>필드 이름은 {@code static/js/result-grading.js} 의 {@code studentTableData} 를 따른다.
 * {@code status} 는 화면 필터(#filterGradingStatus)의 값과 1:1 이어야 하므로 반드시
 * 미채점 / 채점중 / 채점완료 / 확정 네 가지 중 하나다.</p>
 *
 * @param id          화면 표기용 학생 ID(loginId). 개인정보(이메일·전화)는 내리지 않는다.
 * @param scoreStatus 화면 필터(#filterScoreStatus)의 값 — 합격 / 불합격 / 미정
 * @param gradingUrl  채점 팝업 URL
 */
public record AttemptGradingRow(
        int no,
        String attemptId,
        String userId,
        String name,
        String id,
        String time,
        String score,
        String status,
        String scoreStatus,
        boolean disabled,
        String gradingUrl
) {
}
