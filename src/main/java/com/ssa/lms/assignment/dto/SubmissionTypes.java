package com.ssa.lms.assignment.dto;

import com.ssa.lms.assignment.entity.CourseAssignment.SubmissionType;

/**
 * 제출 유형 코드 ↔ 화면 값 변환 한 곳 모음.
 *
 * 화면마다 표기가 다르다:
 *  - 훈련생 제출 모달(trainee/assignments.html) JS: FILE / LINK / TEXT / FILE_TEXT
 *  - 엔티티/IA "데이터 정리": DOCUMENT / LINK / TEXT / DOCUMENT_TEXT
 * JS 의 상수는 손대지 않기로 했으므로(기존 화면 로직 보존) 서버가 맞춰준다.
 */
public final class SubmissionTypes {

    private SubmissionTypes() {
    }

    /** 화면/폼 문자열 → 엔티티 enum. FILE 계열도 받아준다. */
    public static SubmissionType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return SubmissionType.DOCUMENT;
        }
        return switch (raw.trim().toUpperCase()) {
            case "FILE", "DOCUMENT", "문서", "문서 제출" -> SubmissionType.DOCUMENT;
            case "LINK", "링크", "링크 제출" -> SubmissionType.LINK;
            case "TEXT", "텍스트", "텍스트 입력" -> SubmissionType.TEXT;
            case "FILE_TEXT", "DOCUMENT_TEXT", "문서+텍스트" -> SubmissionType.DOCUMENT_TEXT;
            default -> throw new IllegalArgumentException("알 수 없는 제출 유형: " + raw);
        };
    }

    /** 엔티티 enum → 훈련생 화면 JS 가 기대하는 코드. */
    public static String toScreenCode(SubmissionType type) {
        return switch (type) {
            case DOCUMENT -> "FILE";
            case LINK -> "LINK";
            case TEXT -> "TEXT";
            case DOCUMENT_TEXT -> "FILE_TEXT";
        };
    }

    /** 한국어 라벨 — 관리자 목록/상세 표기용. */
    public static String label(SubmissionType type) {
        return switch (type) {
            case DOCUMENT -> "문서 제출";
            case LINK -> "링크 제출";
            case TEXT -> "텍스트 입력";
            case DOCUMENT_TEXT -> "문서+텍스트";
        };
    }

    public static boolean requiresFile(SubmissionType type) {
        return type == SubmissionType.DOCUMENT || type == SubmissionType.DOCUMENT_TEXT;
    }

    public static boolean requiresText(SubmissionType type) {
        return type == SubmissionType.TEXT || type == SubmissionType.DOCUMENT_TEXT;
    }

    public static boolean requiresLink(SubmissionType type) {
        return type == SubmissionType.LINK;
    }
}
