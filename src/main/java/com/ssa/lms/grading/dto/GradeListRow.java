package com.ssa.lms.grading.dto;

/**
 * 성적 목록 한 행 — {@code admin-evaluation-test-grading.html} 표 + CSV 다운로드.
 *
 * <p>CSV 헤더 순서와 이 레코드의 필드 순서를 일치시켜 둔다 (컬럼이 늘 때 한쪽만 고치는 사고 방지).</p>
 */
public record GradeListRow(
        int no,
        String gradeId,
        String userName,
        String loginId,
        Integer autoScore,
        Integer manualScore,
        Integer totalScore,
        String passedText,
        String status,
        String statusText,
        String gradedBy,
        String gradedAt,
        String confirmedBy,
        String confirmedAt,
        int historyCount
) {
}
