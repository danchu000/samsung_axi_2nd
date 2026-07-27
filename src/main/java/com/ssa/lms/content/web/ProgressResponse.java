package com.ssa.lms.content.web;

import com.ssa.lms.content.entity.Progress;

/**
 * 진도 갱신/조회 응답 (서버 → 훈련생 시청 화면, JSON).
 */
public record ProgressResponse(
        Long contentId,
        int progressRate,
        boolean completed,
        Integer lastPositionSeconds,
        Integer maxPageReached
) {
    public static ProgressResponse of(Long contentId, Progress p) {
        if (p == null) {
            return new ProgressResponse(contentId, 0, false, null, null);
        }
        return new ProgressResponse(
                contentId,
                p.getProgressRate(),
                p.isCompleted(),
                p.getLastPositionSeconds(),
                p.getMaxPageReached()
        );
    }
}
