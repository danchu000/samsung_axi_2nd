package com.ssa.lms.survey.dto;

import com.ssa.lms.survey.entity.Survey;

import java.time.format.DateTimeFormatter;

/**
 * 관리자 설문 목록 한 행.
 *
 * 화면(admin-attendance-survey.html) 테이블 헤더와 대응:
 * 상태 / 설문명 / 유형 / 연계 차시 / 이수 반영 / 필수 / 응답률 / 이수현황 / 수강률 / 이탈위험도 / 마감일
 *
 * 이수현황·수강률·이탈위험도는 A(출결/이수) 소유 데이터라 B가 계산하지 않는다. "-" 로 내린다.
 *
 * 엔티티를 그대로 화면에 넘기지 않는 이유: LAZY 프록시 문제 + DTO 경유 원칙(b-entity-mapping.md).
 */
public record SurveyListRow(
        String id,
        /** 화면 값: 활성화 / 비활성화 */
        String status,
        /** 화면 값: 응답중 / 응답대기 / 마감 / 작성중 */
        String surveyStatus,
        String title,
        String type,
        String sessionName,
        /** O / X */
        String reflect,
        /** 필수 / 선택 */
        String required,
        /** "92%" 형태. 대상자를 셀 수 없으면 "-". */
        String response,
        long responseCount,
        long targetCount,
        int questionCount,
        String deadline,
        boolean anonymous
) {

    public static SurveyListRow of(Survey s, long responseCount, long targetCount, long questionCount) {
        return new SurveyListRow(
                String.valueOf(s.getId()),
                s.getStatus() == Survey.SurveyStatus.DRAFT ? "비활성화" : "활성화",
                statusLabel(s.getStatus()),
                s.getTitle(),
                typeLabel(s.getSurveyType()),
                s.getSession() == null ? "-" : s.getSession().getName(),
                s.isReflectCompletion() ? "O" : "X",
                s.isRequired() ? "필수" : "선택",
                formatRate(responseCount, targetCount),
                responseCount,
                targetCount,
                (int) questionCount,
                s.getEndAt().format(DateTimeFormatter.ISO_LOCAL_DATE),
                s.isAnonymous()
        );
    }

    private static String statusLabel(Survey.SurveyStatus status) {
        return switch (status) {
            case DRAFT -> "작성중";
            case SCHEDULED -> "응답대기";
            case ONGOING -> "응답중";
            case CLOSED -> "마감";
        };
    }

    private static String typeLabel(Survey.SurveyType type) {
        return switch (type) {
            case COURSE_SATISFACTION -> "만족도";
            case LECTURE_FEEDBACK -> "평가";
            case FACILITY -> "시설";
            case COMPLETION -> "수료";
            case ETC -> "기타";
        };
    }

    /**
     * 응답률 = 응답 건수 / 대상 과정의 수강생 수.
     * 분모는 CourseQueryService.findUserIdsByCourseId 로 구한다.
     * 과정이 없는 전체 대상 설문은 분모를 셀 방법이 없어 "-" 로 둔다 (A에게 메서드 요청 중).
     */
    private static String formatRate(long responseCount, long targetCount) {
        if (targetCount <= 0) {
            return "-";
        }
        return Math.round(responseCount * 100.0 / targetCount) + "%";
    }
}
