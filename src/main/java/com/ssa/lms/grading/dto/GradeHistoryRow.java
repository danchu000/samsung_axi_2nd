package com.ssa.lms.grading.dto;

/**
 * 성적 변경 이력 한 행 — 화면의 "변경 이력" 탭.
 *
 * <p>모양은 {@code static/js/assignments-grading.js} 의 {@code gradingHistoryData}
 * ({@code { date, instructor, score: '65 → 75', result, reason }}) 를 따른다.</p>
 */
public record GradeHistoryRow(
        String gradeId,
        String userName,
        String date,
        String instructor,
        String score,
        String result,
        String reason
) {
}
