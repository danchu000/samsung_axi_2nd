package com.ssa.lms.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 대시보드 공통 표시 포맷.
 *
 * <p>대시보드 DTO 는 전부 <b>문자열</b>로 값을 내린다. 세 화면 모두 값을 그대로
 * 템플릿 문자열에 끼워 넣는 JS 렌더러(dashboard.js / trainee/index.js / admin/index.html 인라인)를
 * 쓰기 때문이다 — 숫자·날짜를 객체로 내리면 직렬화 표현이 화면마다 갈린다 (HANDOFF §8-4).</p>
 */
public final class DashboardFormat {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String[] DAY_OF_WEEK = {"월", "화", "수", "목", "금", "토", "일"};

    /** 값이 없다는 뜻으로 화면에 그대로 찍는 문자열. "0" 과 구분해야 한다. */
    public static final String NONE = "-";

    private DashboardFormat() {
    }

    /** "2026.07.28 (화)" */
    public static String today(LocalDate date) {
        return date.format(DATE) + " (" + DAY_OF_WEEK[date.getDayOfWeek().getValue() - 1] + ")";
    }

    public static String date(LocalDate date) {
        return date == null ? NONE : date.format(DATE);
    }

    public static String date(LocalDateTime at) {
        return at == null ? NONE : at.format(DATE);
    }

    public static String dateTime(LocalDateTime at) {
        return at == null ? NONE : at.format(DATE_TIME);
    }

    public static String isoDate(LocalDate date) {
        return date == null ? NONE : date.format(ISO_DATE);
    }

    /** 남은 일수. 지났으면 음수. 날짜가 없으면 null(화면이 D-day 배지를 생략한다). */
    public static Integer daysUntil(LocalDate from, LocalDate target) {
        return target == null ? null : (int) ChronoUnit.DAYS.between(from, target);
    }

    public static Integer daysUntil(LocalDate from, LocalDateTime target) {
        return target == null ? null : daysUntil(from, target.toLocalDate());
    }

    /** 퍼센트 표시. null 이면 "-" (0% 와 구분한다). */
    public static String percent(Integer value) {
        return value == null ? NONE : value + "%";
    }

    /** 정수 표시. null 이면 "-". */
    public static String number(Integer value) {
        return value == null ? NONE : String.valueOf(value);
    }

    public static String number(Long value) {
        return value == null ? NONE : String.valueOf(value);
    }

    public static String nullToDash(String v) {
        return (v == null || v.isBlank()) ? NONE : v;
    }
}
