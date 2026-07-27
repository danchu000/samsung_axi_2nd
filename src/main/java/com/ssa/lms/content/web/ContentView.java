package com.ssa.lms.content.web;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.ContentType;

import java.time.LocalDateTime;

/**
 * 콘텐츠 목록/상세 표시용 뷰 DTO — OSIV 비활성 환경에서 템플릿이 lazy 연관을 건드리지 않도록
 * 서비스 트랜잭션 안에서 필요한 스칼라 값을 모두 뽑아 담는다.
 */
public record ContentView(
        Long id,
        ContentType type,
        String typeLabel,
        String title,
        String description,
        Long courseId,
        String courseName,
        Long sessionId,
        String sessionName,
        Integer durationSeconds,
        Integer pageCount,
        String durationLabel,
        int orderNo,
        boolean required,
        ContentStatus status,
        String statusLabel,
        String fileUrl,
        String originalFileName,
        LocalDateTime createdAt
) {

    public static ContentView of(Content c) {
        return new ContentView(
                c.getId(),
                c.getType(),
                c.getType().getLabel(),
                c.getTitle(),
                c.getDescription(),
                c.getCourse().getId(),
                c.getCourse().getCourseName(),
                c.getSession() != null ? c.getSession().getId() : null,
                c.getSession() != null ? c.getSession().getName() : null,
                c.getDurationSeconds(),
                c.getPageCount(),
                durationLabel(c),
                c.getOrderNo(),
                c.isRequired(),
                c.getStatus(),
                c.getStatus().getLabel(),
                c.getFileUrl(),
                c.getOriginalFileName(),
                c.getCreatedAt()
        );
    }

    private static String durationLabel(Content c) {
        if (c.getType() == ContentType.VIDEO && c.getDurationSeconds() != null) {
            int total = c.getDurationSeconds();
            int m = total / 60;
            int s = total % 60;
            return m > 0 ? m + "분 " + s + "초" : s + "초";
        }
        if (c.getType() == ContentType.DOCUMENT && c.getPageCount() != null) {
            return c.getPageCount() + "페이지";
        }
        return "-";
    }
}
