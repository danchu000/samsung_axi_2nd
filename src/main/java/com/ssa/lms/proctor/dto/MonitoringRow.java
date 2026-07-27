package com.ssa.lms.proctor.dto;

/**
 * 모니터링 목록 한 행 — static/js/exams.js 의 {@code testList} 더미와 같은 shape 이다.
 *
 * <p>날짜/시각을 문자열로 내리는 이유는 화면 JS 가 {@code data-start}/{@code data-end} 를
 * {@code new Date(...)} 로 파싱하고 문자열 비교를 하기 때문이다 (HANDOFF §8-4).</p>
 *
 * @param inProgress 응시중 인원 (ExamAttempt.status = IN_PROGRESS)
 * @param completed  완료 인원 (SUBMITTED / AUTO_SUBMITTED)
 * @param notStarted 미응시 인원 (수강생 수 - 응시 기록이 있는 인원)
 * @param voided     무효 처리 인원. 화면에는 별도 표기하지만 완료/미응시에는 넣지 않는다.
 */
public record MonitoringRow(
        int number,
        Long examId,
        String courseName,
        String courseId,
        String testName,
        String instructor,
        String date,
        String time,
        String timeRemaining,
        String timeElapsed,
        int inProgress,
        int completed,
        int notStarted,
        int voided,
        int enrolled,
        String start,
        String end,
        String detailUrl
) {
}
