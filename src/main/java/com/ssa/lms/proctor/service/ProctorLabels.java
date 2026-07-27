package com.ssa.lms.proctor.service;

import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.proctor.entity.ExamEventLog;
import com.ssa.lms.proctor.entity.ExamRecording;

import java.time.format.DateTimeFormatter;

/** 화면 표기 문자열 모음. 엔티티에 UI 문구를 넣지 않으려고 한곳에 모았다. */
public final class ProctorLabels {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    /** 화면 JS 가 new Date() 로 파싱하는 형식 (exams.js 의 data-start/data-end). */
    public static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private ProctorLabels() {
    }

    public static String attemptStatus(ExamAttempt.AttemptStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case NOT_STARTED -> "미응시";
            case IN_PROGRESS -> "응시중";
            case SUBMITTED -> "제출완료";
            case AUTO_SUBMITTED -> "자동제출";
            case ABANDONED -> "응시중단";
            case VOIDED -> "무효처리";
        };
    }

    public static String eventType(ExamEventLog.EventType type) {
        if (type == null) {
            return "-";
        }
        return switch (type) {
            case ENTER -> "입장";
            case EXIT -> "퇴장/제출";
            case RESUME -> "재접속";
            case TAB_BLUR -> "탭 이탈";
            case TAB_FOCUS -> "탭 복귀";
            case FULLSCREEN_EXIT -> "전체화면 해제";
            case COPY -> "복사";
            case PASTE -> "붙여넣기";
            case MULTI_FACE -> "얼굴 다중 감지";
            case NO_FACE -> "얼굴 미감지";
            case NETWORK_DROP -> "네트워크 끊김";
        };
    }

    public static String severity(ExamEventLog.Severity severity) {
        if (severity == null) {
            return "-";
        }
        return switch (severity) {
            case INFO -> "정보";
            case WARN -> "주의";
            case CRITICAL -> "심각";
        };
    }

    public static String recordingStatus(ExamRecording.RecordingStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case RECORDING -> "녹화중";
            case PROCESSING -> "처리중";
            case AVAILABLE -> "재생가능";
            case FAILED -> "실패";
            case PURGED -> "보존기간 경과";
        };
    }

    /** 초 → HH:mm:ss. 음수는 00:00:00. */
    public static String duration(long seconds) {
        long s = Math.max(seconds, 0);
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    public static String fileSize(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "-";
        }
        double mb = bytes / (1024.0 * 1024.0);
        return mb >= 1024
                ? String.format("%.1f GB", mb / 1024.0)
                : String.format("%.1f MB", mb);
    }
}
