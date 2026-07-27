package com.ssa.lms.support.dto;

import com.ssa.lms.support.entity.Qna;

import java.time.LocalDateTime;

/**
 * Q&A 목록 한 행.
 *
 * 화면 대응:
 *  - admin-support-tutoring.html Q&A 탭 테이블: 상태 / 질문 제목 / 작성자 / 등록일 / 조회수 / 관리
 *  - trainee/qna.html #qnaTbody: 번호 / 상태+제목(+자물쇠) / 등록일 / 조회수
 *
 * 엔티티를 그대로 넘기지 않는 이유: content 가 개인정보(AES-256) 라서
 * 목록 HTML 에 실려 나가면 안 되고, LAZY 프록시 문제도 피해야 하기 때문.
 * <b>이 DTO 에는 content 필드가 없다.</b>
 */
public record QnaListRow(
        Long id,
        String title,
        /** 화면 라벨: 미답변 / 진행중 / 답변완료 / 종료 */
        String statusLabel,
        /** 화면 CSS 클래스: status-dropout / status-pending / status-in-progress / status-disabled */
        String statusClass,
        /** trainee 화면의 data-status 값 */
        String statusCode,
        String categoryLabel,
        String authorName,
        String courseName,
        Integer sessionSeq,
        String courseSession,
        String createdAt,
        String createdAtShort,
        int viewCount,
        boolean secret,
        String assigneeName,
        boolean assigned,
        String elapsed
) {

    public static QnaListRow of(Qna q, LocalDateTime now) {
        return new QnaListRow(
                q.getId(),
                q.getTitle(),
                statusLabel(q.getStatus()),
                statusClass(q.getStatus()),
                statusCode(q.getStatus()),
                categoryLabel(q.getCategory()),
                q.getUser() == null ? "-" : q.getUser().getName(),
                q.getCourse() == null ? null : q.getCourse().getCourseName(),
                q.getSession() == null ? null : q.getSession().getSeq(),
                SupportFormat.courseSession(
                        q.getCourse() == null ? null : q.getCourse().getCourseName(),
                        q.getSession() == null ? null : q.getSession().getSeq()),
                SupportFormat.date(q.getCreatedAt()),
                SupportFormat.shortDate(q.getCreatedAt()),
                q.getViewCount(),
                q.isSecret(),
                q.getAssignee() == null ? "미배정" : q.getAssignee().getName(),
                q.getAssignee() != null,
                SupportFormat.elapsed(q.getCreatedAt(), q.getFirstResponseAt(), now)
        );
    }

    public static String statusLabel(Qna.QnaStatus status) {
        return switch (status) {
            case WAITING -> "미답변";
            case IN_PROGRESS -> "진행중";
            case ANSWERED -> "답변완료";
            case CLOSED -> "종료";
        };
    }

    /** 기존 화면이 쓰던 status-badge 클래스에 맞춘다. */
    public static String statusClass(Qna.QnaStatus status) {
        return switch (status) {
            case WAITING -> "status-dropout";
            case IN_PROGRESS -> "status-pending";
            case ANSWERED -> "status-in-progress";
            case CLOSED -> "status-disabled";
        };
    }

    /** trainee/qna.html 의 data-status (JS 필터가 쓰는 값). */
    public static String statusCode(Qna.QnaStatus status) {
        return switch (status) {
            case WAITING -> "pending";
            case IN_PROGRESS -> "progress";
            case ANSWERED -> "answered";
            case CLOSED -> "closed";
        };
    }

    public static String categoryLabel(Qna.QnaCategory category) {
        if (category == null) {
            return "-";
        }
        return switch (category) {
            case LECTURE -> "수업";
            case PROGRESS -> "진도";
            case ETC -> "기타";
        };
    }
}
