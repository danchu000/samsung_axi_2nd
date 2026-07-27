package com.ssa.lms.assignment.dto;

import com.ssa.lms.grading.entity.GradeHistory;

import java.time.format.DateTimeFormatter;

/**
 * 채점 화면 "변경 이력" 탭 한 행.
 * static/js/assignments-grading.js 의 gradingHistoryData 더미 shape
 * ({ date, instructor, score: '65 → 75', result, reason }) 을 그대로 따른다.
 *
 * 성적 정정은 내역서 증빙 요건이라 사유 없이 남는 행이 있으면 안 된다.
 */
public record GradeHistoryRow(
        String date,
        String instructor,
        String score,
        String result,
        String reason
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static GradeHistoryRow of(GradeHistory h) {
        return new GradeHistoryRow(
                h.getChangedAt() == null ? "-" : h.getChangedAt().format(FMT),
                h.getChangedBy().getName(),
                nullSafe(h.getBeforeScore()) + " → " + nullSafe(h.getAfterScore()),
                Boolean.TRUE.equals(h.getAfterPassed()) ? "통과" : "미통과",
                h.getReason()
        );
    }

    private static String nullSafe(Integer v) {
        return v == null ? "-" : String.valueOf(v);
    }
}
