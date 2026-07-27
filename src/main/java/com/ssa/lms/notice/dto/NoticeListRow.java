package com.ssa.lms.notice.dto;

import com.ssa.lms.notice.entity.Notice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 공지 목록 한 행.
 *
 * 화면(admin-07-notice/admin-notice.html) 테이블 헤더와 1:1 대응:
 * 번호 / 카테고리 / 제목 / 작성자 / 작성일 / 수정일 / 조회수
 *
 * 원본 더미: static/js/notices.js 의 noticeData
 * (id, category, title, author, date, editDate, views)
 *
 * 엔티티를 그대로 넘기지 않는 이유: open-in-view=false 라 LAZY 프록시가 화면에서 터진다.
 */
public record NoticeListRow(
        Long id,
        String category,
        String title,
        String author,
        String date,
        String editDate,
        int views,
        boolean pinned,
        /** null = 전체 공지. 값이 있으면 해당 과정 수강생 한정. */
        String courseName
) {

    public static NoticeListRow of(Notice n) {
        return new NoticeListRow(
                n.getId(),
                n.getCategory() == null ? "-" : n.getCategory().getName(),
                n.getTitle(),
                n.getAuthor().getName(),
                formatDate(n.getCreatedAt()),
                formatDate(n.getUpdatedAt()),
                n.getViewCount() == null ? 0 : n.getViewCount(),
                n.isPinned(),
                n.getCourse() == null ? null : n.getCourse().getCourseName()
        );
    }

    private static String formatDate(LocalDateTime at) {
        return at == null ? "-" : at.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
