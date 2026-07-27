package com.ssa.lms.support.dto;

import com.ssa.lms.support.entity.Qna;
import com.ssa.lms.support.entity.QnaAnswer;

import java.util.List;

/**
 * Q&A 상세.
 *
 * 화면 대응: admin-06-support/admin-support-qna.html (관리자 상세 + 답변 작성),
 *           trainee/qna-detail.html, instructor/qna-detail.html
 *
 * content 는 복호화된 평문이다. 화면에 뿌려야 하므로 여기에만 담고,
 * 목록 DTO({@link QnaListRow})에는 절대 넣지 않는다.
 */
public record QnaDetail(
        Long id,
        String title,
        String content,
        String statusLabel,
        String statusClass,
        String statusCode,
        String categoryLabel,
        String authorName,
        Long authorId,
        String courseName,
        String courseSession,
        String createdAt,
        int viewCount,
        boolean secret,
        String assigneeName,
        boolean assigned,
        String firstResponseAt,
        String closedAt,
        String elapsed,
        List<AnswerRow> answers
) {

    /** 답변 한 건. */
    public record AnswerRow(
            Long id,
            String content,
            String responderName,
            String responderRole,
            String createdAt
    ) {
        public static AnswerRow of(QnaAnswer a) {
            return new AnswerRow(
                    a.getId(),
                    a.getContent(),
                    a.getResponder() == null ? "-" : a.getResponder().getName(),
                    a.getResponder() == null || a.getResponder().getRole() == null
                            ? "-" : a.getResponder().getRole().getLabel(),
                    SupportFormat.dateTime(a.getCreatedAt())
            );
        }
    }

    public static QnaDetail of(Qna q, List<QnaAnswer> answers, java.time.LocalDateTime now) {
        return new QnaDetail(
                q.getId(),
                q.getTitle(),
                q.getContent(),
                QnaListRow.statusLabel(q.getStatus()),
                QnaListRow.statusClass(q.getStatus()),
                QnaListRow.statusCode(q.getStatus()),
                QnaListRow.categoryLabel(q.getCategory()),
                q.getUser() == null ? "-" : q.getUser().getName(),
                q.getUser() == null ? null : q.getUser().getId(),
                q.getCourse() == null ? null : q.getCourse().getCourseName(),
                SupportFormat.courseSession(
                        q.getCourse() == null ? null : q.getCourse().getCourseName(),
                        q.getSession() == null ? null : q.getSession().getSeq()),
                SupportFormat.dateTime(q.getCreatedAt()),
                q.getViewCount(),
                q.isSecret(),
                q.getAssignee() == null ? "미배정" : q.getAssignee().getName(),
                q.getAssignee() != null,
                SupportFormat.dateTime(q.getFirstResponseAt()),
                SupportFormat.dateTime(q.getClosedAt()),
                SupportFormat.elapsed(q.getCreatedAt(), q.getFirstResponseAt(), now),
                answers.stream().map(AnswerRow::of).toList()
        );
    }
}
