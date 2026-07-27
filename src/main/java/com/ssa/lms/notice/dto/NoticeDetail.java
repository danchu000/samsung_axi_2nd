package com.ssa.lms.notice.dto;

import com.ssa.lms.notice.entity.Notice;
import com.ssa.lms.notice.entity.NoticeAttachment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 공지 상세 (notice-detail.html / trainee·instructor 상세).
 *
 * 원본 더미: static/js/notices-detail.js 의 noticeDetail 객체
 * (id, title, author, date, editDate, editor, views, prev, next, attachments, content)
 *
 * open-in-view=false 라 LAZY 연관(첨부·작성자·과정)은 전부 서비스 트랜잭션 안에서 펼쳐 담는다.
 */
public record NoticeDetail(
        Long id,
        String category,
        String title,
        String content,
        String author,
        String date,
        String editDate,
        String editor,
        int views,
        boolean pinned,
        boolean published,
        String courseName,
        List<Attachment> attachments,
        Nav prev,
        Nav next
) {

    public record Attachment(Long id, String name, String url, long sizeBytes) {
    }

    /** 이전 글 / 다음 글 링크. */
    public record Nav(Long id, String title, String date) {
    }

    public static NoticeDetail of(Notice n, Notice prev, Notice next) {
        return new NoticeDetail(
                n.getId(),
                n.getCategory() == null ? "-" : n.getCategory().getName(),
                n.getTitle(),
                n.getContent(),
                n.getAuthor().getName(),
                formatDate(n.getCreatedAt()),
                formatDate(n.getUpdatedAt()),
                // 수정자 이름은 A의 User 를 한 번 더 조회해야 해서, 아직은 작성자로 대체한다.
                // (BaseEntity.updatedBy 는 user id 만 갖는다 — a-requests.md P0-2)
                n.getAuthor().getName(),
                n.getViewCount() == null ? 0 : n.getViewCount(),
                n.isPinned(),
                n.getPublishedAt() != null,
                n.getCourse() == null ? null : n.getCourse().getCourseName(),
                n.getAttachments().stream().map(NoticeDetail::toAttachment).toList(),
                toNav(prev),
                toNav(next)
        );
    }

    private static Attachment toAttachment(NoticeAttachment a) {
        return new Attachment(a.getId(), a.getOriginalName(), a.getStoredPath(),
                a.getSizeBytes() == null ? 0L : a.getSizeBytes());
    }

    private static Nav toNav(Notice n) {
        return n == null ? null : new Nav(n.getId(), n.getTitle(), formatDate(n.getCreatedAt()));
    }

    private static String formatDate(LocalDateTime at) {
        return at == null ? "-" : at.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
