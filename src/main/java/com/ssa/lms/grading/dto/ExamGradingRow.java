package com.ssa.lms.grading.dto;

/**
 * 시험 채점 목록 한 행 — {@code admin-evaluation-result.html} / {@code instructor/result.html}.
 *
 * <p>필드 이름은 화면이 이미 쓰는 {@code static/js/assignments.js} 의 {@code gradingData} 항목 모양을
 * 그대로 따른다. 그래야 렌더 함수(initEvaluationList)를 손대지 않고 서버 데이터로만 갈아끼울 수 있다.</p>
 *
 * <p>숫자·날짜를 전부 문자열로 내리는 이유: 화면 JS 가 문자열 비교/템플릿 문자열로 쓰기 때문이다
 * (HANDOFF §8-4 — LocalDate 직렬화 이슈).</p>
 *
 * @param status  assignments.js 의 상태 키. waiting(평가예정) / pending(채점예정) / completed(채점완료)
 * @param address 이 시험의 채점 화면 URL. assignments.js 가 버튼 클릭 시 이동한다.
 */
public record ExamGradingRow(
        int number,
        String examId,
        String courseName,
        String courseId,
        String title,
        String instructor,
        String evalType,
        String startDate,
        String startTime,
        String endDate,
        String endTime,
        int submitted,
        int notSubmitted,
        String status,
        String address,
        int graded,
        int ungraded
) {
}
