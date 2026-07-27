package com.ssa.lms.notice.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 공지 목록 검색 조건.
 *
 * 화면 셀렉트는 "전체"를 빈 문자열로 넘긴다. 컨트롤러에서 그대로 받아 여기서 null 로 정규화한다
 * (null = 조건 미적용). QuestionSearchCond 와 같은 방식.
 *
 * @param dateFilter 화면 값 "" / today / this-week / this-month / custom
 */
public record NoticeSearchCond(
        Long categoryId,
        String dateFilter,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        String keyword
) {

    private static boolean isAll(String v) {
        return v == null || v.isBlank() || "전체".equals(v);
    }

    public Long categoryIdOrNull() {
        return categoryId;
    }

    public String keywordOrNull() {
        return isAll(keyword) ? null : keyword.trim();
    }

    /** 조회 범위 시작 (포함). null 이면 제한 없음. */
    public LocalDateTime fromOrNull() {
        LocalDate today = LocalDate.now();
        return switch (dateFilter == null ? "" : dateFilter) {
            case "today" -> today.atStartOfDay();
            case "this-week" -> today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay();
            case "this-month" -> today.withDayOfMonth(1).atStartOfDay();
            case "custom" -> startDate == null ? null : startDate.atStartOfDay();
            default -> null;
        };
    }

    /** 조회 범위 끝 (미포함). 종료일은 그 날 하루를 포함해야 하므로 +1일 한 00:00 을 쓴다. */
    public LocalDateTime toOrNull() {
        if (!"custom".equals(dateFilter)) {
            return null;
        }
        return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }
}
