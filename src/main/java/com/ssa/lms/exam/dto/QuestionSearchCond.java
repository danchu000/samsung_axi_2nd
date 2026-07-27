package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.Difficulty;
import com.ssa.lms.exam.entity.Question;

/**
 * 문제은행 검색 조건.
 *
 * 화면 셀렉트는 "전체"를 빈 문자열 또는 "전체" 문자열로 넘긴다.
 * 컨트롤러에서 그대로 받아 여기서 null 로 정규화한다 (null = 조건 미적용).
 *
 * @param type 화면 탭/유형 필터(영상·문서·강의·과제·문제·시험). B는 "문제"만 다루므로
 *             나머지 유형은 A의 콘텐츠 목록과 병합될 때 쓰인다.
 */
public record QuestionSearchCond(
        String type,
        String keyword,
        String difficulty,
        String category,
        String status
) {

    private static boolean isAll(String v) {
        return v == null || v.isBlank() || "전체".equals(v);
    }

    public String keywordOrNull() {
        return isAll(keyword) ? null : keyword.trim();
    }

    public String categoryOrNull() {
        return isAll(category) ? null : category;
    }

    public Difficulty difficultyOrNull() {
        if (isAll(difficulty)) {
            return null;
        }
        return Difficulty.valueOf(difficulty.toUpperCase());
    }

    /** 화면 값 Active / Archived → QuestionStatus.ACTIVE / INACTIVE */
    public Question.QuestionStatus statusOrNull() {
        if (isAll(status)) {
            return null;
        }
        return switch (status) {
            case "Active", "ACTIVE" -> Question.QuestionStatus.ACTIVE;
            case "Archived", "INACTIVE" -> Question.QuestionStatus.INACTIVE;
            default -> throw new IllegalArgumentException("알 수 없는 상태 값: " + status);
        };
    }

    /** "문제" 유형이 조회 대상인지. 다른 유형만 선택된 경우 문제은행은 비어야 한다. */
    public boolean includesQuestionType() {
        return isAll(type) || "문제".equals(type);
    }
}
