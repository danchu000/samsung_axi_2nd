package com.ssa.lms.notice.dto;

import com.ssa.lms.notice.entity.Notification;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 알림 내역 검색 조건 (admin-alarm.html 필터).
 * 화면의 "카테고리" 셀렉트는 중요도(높음/중간/낮음)로 쓴다 — 알림에는 공지 분류가 없다.
 */
public record NotificationSearchCond(
        String priority,
        String dateFilter,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        String keyword
) {

    private static boolean isAll(String v) {
        return v == null || v.isBlank() || "전체".equals(v);
    }

    public String keywordOrNull() {
        return isAll(keyword) ? null : keyword.trim();
    }

    public Notification.Priority priorityOrNull() {
        if (isAll(priority)) {
            return null;
        }
        return switch (priority) {
            case "긴급", "URGENT" -> Notification.Priority.URGENT;
            case "높음", "HIGH", "high" -> Notification.Priority.HIGH;
            case "중간", "보통", "NORMAL", "normal" -> Notification.Priority.NORMAL;
            case "낮음", "LOW", "low" -> Notification.Priority.LOW;
            default -> throw new IllegalArgumentException("알 수 없는 중요도 값: " + priority);
        };
    }

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

    public LocalDateTime toOrNull() {
        if (!"custom".equals(dateFilter)) {
            return null;
        }
        return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }
}
