package com.ssa.lms.survey.dto;

import com.ssa.lms.survey.entity.Survey;

import java.time.format.DateTimeFormatter;

/**
 * 훈련생 설문 목록 한 행.
 *
 * static/js/trainee/surveys.js 의 SURVEYS 더미 배열과 같은 shape 이다 —
 * 그 파일이 window._serverSurveyRows 를 읽어 그대로 렌더링한다.
 *
 * status 는 DB 컬럼이 아니라 파생 상태다. {@link #derive} 참고.
 */
public record TraineeSurveyRow(
        String id,
        String title,
        String course,
        String cohort,
        String startAt,
        String endAt,
        int questionCount,
        /** ONGOING / SUBMITTED / CLOSED_UNANSWERED / SCHEDULED */
        String status,
        String createdAt
) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public static TraineeSurveyRow of(Survey s, boolean responded, long questionCount,
                                      java.time.LocalDateTime now) {
        return new TraineeSurveyRow(
                String.valueOf(s.getId()),
                s.getTitle(),
                s.getCourse() == null ? "전체" : s.getCourse().getCourseName(),
                s.getCourse() == null ? "-" : nullToDash(s.getCourse().getCohort()),
                s.getStartAt().format(DATE),
                s.getEndAt().format(DATE),
                (int) questionCount,
                derive(s, responded, now),
                s.getCreatedAt() == null ? "-" : s.getCreatedAt().format(DATE)
        );
    }

    /**
     * 파생 상태 규칙.
     *
     *  1. 이미 응답했으면 무조건 SUBMITTED — 기간이 끝났어도 "종료(미응답)"이 아니다.
     *  2. 아직 시작 전이면 SCHEDULED.
     *  3. 마감(status=CLOSED 이거나 endAt 이 지남)이면 CLOSED_UNANSWERED.
     *  4. 그 외는 ONGOING.
     *
     * SUBMITTED / CLOSED_UNANSWERED 는 컬럼이 아니라 (Survey.status, 그 사용자의
     * SurveyResponse 존재 여부) 조합이다 — Survey 엔티티 주석 참고.
     *
     * status 만으로 판정하지 않는 이유: 배치가 없으면 기간이 지나도 status 가 ONGOING 에
     * 머문다. 그래서 날짜도 함께 본다.
     */
    public static String derive(Survey s, boolean responded, java.time.LocalDateTime now) {
        if (responded) {
            return "SUBMITTED";
        }
        if (s.getStatus() == Survey.SurveyStatus.DRAFT || now.isBefore(s.getStartAt())) {
            return "SCHEDULED";
        }
        if (s.getStatus() == Survey.SurveyStatus.CLOSED || now.isAfter(s.getEndAt())) {
            return "CLOSED_UNANSWERED";
        }
        return "ONGOING";
    }

    private static String nullToDash(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
