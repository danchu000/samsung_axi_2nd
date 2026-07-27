package com.ssa.lms.survey.dto;

import com.ssa.lms.survey.entity.Survey;

/**
 * 관리자 설문 목록 검색 조건.
 *
 * 화면 셀렉트는 "전체"를 빈 문자열 또는 "전체" 문자열로 넘긴다.
 * 컨트롤러에서 그대로 받아 여기서 null 로 정규화한다 (null = 조건 미적용).
 */
public record SurveySearchCond(
        Long courseId,
        String status,
        String keyword
) {

    private static boolean isAll(String v) {
        return v == null || v.isBlank() || "전체".equals(v) || "ALL".equals(v);
    }

    public String keywordOrNull() {
        return isAll(keyword) ? null : keyword.trim();
    }

    /** 화면 값(응답중/응답대기/마감/작성중)과 enum 이름을 모두 받는다. */
    public Survey.SurveyStatus statusOrNull() {
        if (isAll(status)) {
            return null;
        }
        return switch (status) {
            case "작성중", "DRAFT" -> Survey.SurveyStatus.DRAFT;
            case "응답대기", "예정", "SCHEDULED" -> Survey.SurveyStatus.SCHEDULED;
            case "응답중", "진행중", "ONGOING" -> Survey.SurveyStatus.ONGOING;
            case "마감", "종료", "CLOSED" -> Survey.SurveyStatus.CLOSED;
            default -> throw new IllegalArgumentException("알 수 없는 설문 상태 값: " + status);
        };
    }
}
