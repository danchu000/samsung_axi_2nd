package com.ssa.lms.survey.dto;

import java.util.List;

/**
 * 설문 결과 리포트 — 문항별 집계.
 *
 * <p>엔티티를 그대로 넘기지 않는다(LAZY 프록시 + 응답자 신원 노출 방지).
 * 응답자 이름·아이디는 이 DTO 어디에도 없다 — 리포트는 "누가 뭐라고 했나"가 아니라
 * "무슨 답이 몇 건인가"를 보는 물건이고, 익명 설문이 섞여 있어 한쪽만 이름이 나오면
 * 그 자체가 신원 단서가 된다.</p>
 *
 * @param targetCount   대상 과정 수강생 수. 전체 대상 설문(course=null)은 0 → 응답률 "-"
 * @param responseCount 제출 건수
 */
public record SurveyReportView(
        String surveyId,
        String title,
        String type,
        String courseName,
        String sessionName,
        String surveyStatus,
        String period,
        boolean anonymous,
        boolean required,
        boolean reflectCompletion,
        long targetCount,
        long responseCount,
        String responseRate,
        List<QuestionReport> questions
) {

    /**
     * 문항 하나의 집계.
     *
     * @param respondentCount 이 문항에 답한 <b>사람</b> 수 (복수 선택이어도 1명은 1)
     * @param summary         한 줄 요약. 척도는 "평균 4.2 / 5점", 주관식은 "n건", 선택형은 최다 응답
     * @param options         보기별·척도값별 집계. 주관식은 빈 목록
     * @param texts           주관식 원문. 그 외 유형은 빈 목록
     */
    public record QuestionReport(
            int seq,
            String type,
            String content,
            boolean required,
            long respondentCount,
            String summary,
            List<Option> options,
            List<String> texts
    ) {
    }

    /** 보기 한 줄. {@code ratio} 는 응답자 수 대비 비율 문자열("40%"), 분모가 0이면 "-". */
    public record Option(String label, long count, String ratio) {
    }
}
