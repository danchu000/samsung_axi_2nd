package com.ssa.lms.support.dto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * support 도메인 DTO 공용 표시 포맷.
 *
 * 화면 문자열 규칙을 한 곳에 모아 둔다 (목록·상세·응답현황이 같은 표기를 쓴다).
 */
public final class SupportFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private SupportFormat() {
    }

    public static String date(LocalDateTime at) {
        return at == null ? "-" : at.format(DATE);
    }

    public static String dateTime(LocalDateTime at) {
        return at == null ? "-" : at.format(DATE_TIME);
    }

    /** 목록의 "03/12" 형태. */
    public static String shortDate(LocalDateTime at) {
        return at == null ? "-" : at.format(SHORT_DATE);
    }

    /** 채팅 말풍선의 "10:03" 형태. */
    public static String time(LocalDateTime at) {
        return at == null ? "" : at.format(TIME);
    }

    /**
     * 경과시간 표기 — 화면(static/js/tutoring.js)의 "26h".
     *
     * <p>계산 규칙: 첫 응답이 아직 없으면 {@code now - createdAt},
     * 응답이 있으면 {@code firstResponseAt - createdAt}.
     * 즉 "응답까지 걸린 시간"이고, 미응답 건은 계속 늘어난다.</p>
     */
    public static String elapsed(LocalDateTime createdAt, LocalDateTime firstResponseAt, LocalDateTime now) {
        if (createdAt == null) {
            return "-";
        }
        LocalDateTime end = firstResponseAt != null ? firstResponseAt : now;
        long minutes = Duration.between(createdAt, end).toMinutes();
        if (minutes < 0) {
            minutes = 0;
        }
        if (minutes < 60) {
            return minutes + "m";
        }
        return (minutes / 60) + "h";
    }

    /** 경과 분 — 정렬용 원시값. */
    public static long elapsedMinutes(LocalDateTime createdAt, LocalDateTime firstResponseAt, LocalDateTime now) {
        if (createdAt == null) {
            return 0;
        }
        LocalDateTime end = firstResponseAt != null ? firstResponseAt : now;
        return Math.max(0, Duration.between(createdAt, end).toMinutes());
    }

    /** "1.8시간" 형태 — 카드의 평균 응답 시간. */
    public static String hours(double minutes) {
        return String.format("%.1f시간", minutes / 60.0);
    }

    /** 과정/차시 합성 표기 — 화면의 "AI 분석 / 3차시". */
    public static String courseSession(String courseName, Integer sessionSeq) {
        if (courseName == null && sessionSeq == null) {
            return "-";
        }
        if (sessionSeq == null) {
            return courseName == null ? "-" : courseName;
        }
        return (courseName == null ? "-" : courseName) + " / " + sessionSeq + "차시";
    }
}
