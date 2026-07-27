package com.ssa.lms.grading.dto;

/**
 * 시험별 채점 현황 카드 — {@code result-grading.html} 상단 {@code #examInfoCardContainer}.
 *
 * <p>필드 이름은 {@code static/js/result-grading.js} 의 {@code examInfoData} 를 그대로 따른다
 * (렌더 함수 renderExamInfoCard 를 수정하지 않기 위해서다).</p>
 *
 * @param total    응시(제출)한 훈련생 수. 회차가 여러 번이어도 사람 기준 1명이다.
 * @param graded   채점완료 + 확정 인원
 * @param ungraded 미채점 + 채점중 인원
 */
public record ExamGradingSummary(
        String examId,
        String courseName,
        String examName,
        String instructor,
        String startDate,
        String endDate,
        String status,
        String statusClass,
        int total,
        int graded,
        int ungraded,
        int confirmed,
        int enrolled,
        int passScore,
        int maxScore,
        int manualQuestionCount,
        /** 로그인한 사용자가 이 시험을 채점할 수 있는지 (강사는 담당 과정만). */
        boolean canGrade,
        String gradesUrl,
        String csvUrl
) {
}
