package com.ssa.lms.content.web;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentType;
import com.ssa.lms.content.entity.Progress;

/**
 * 훈련생 학습 화면(콘텐츠 목록/차시별 아코디언)용 뷰 — 콘텐츠 메타 + 내 진도.
 * 기존 프론트 더미(contentId/type/typeLabel/progressRate/isCompleted 등) 구조에 맞춘다.
 */
public record LearningContentView(
        Long contentId,
        ContentType type,
        String typeLabel,
        String title,
        Long courseId,
        String courseName,
        Long sessionId,
        String sessionName,
        int sessionSeq,
        Integer durationSeconds,
        int durationMinutes,
        int progressRate,
        boolean completed,
        Integer lastPositionSeconds,
        Integer maxPageReached,
        String fileUrl
) {

    public static LearningContentView of(Content c, Progress p) {
        Integer dur = c.getDurationSeconds();
        return new LearningContentView(
                c.getId(),
                c.getType(),
                c.getType().getLabel(),
                c.getTitle(),
                c.getCourse().getId(),
                c.getCourse().getCourseName(),
                c.getSession() != null ? c.getSession().getId() : null,
                c.getSession() != null ? c.getSession().getName() : null,
                c.getSession() != null ? c.getSession().getSeq() : 0,
                dur,
                dur != null ? dur / 60 : 0,
                p != null ? p.getProgressRate() : 0,
                p != null && p.isCompleted(),
                p != null ? p.getLastPositionSeconds() : null,
                p != null ? p.getMaxPageReached() : null,
                c.getFileUrl()
        );
    }
}
