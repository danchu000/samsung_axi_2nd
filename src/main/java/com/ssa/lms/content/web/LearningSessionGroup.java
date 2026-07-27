package com.ssa.lms.content.web;

import java.util.List;

/**
 * 훈련생 학습 화면의 차시별 그룹 — 차시 아코디언 렌더링용(차시 평균 진도율 포함).
 * 차시 미지정(과정 공용) 콘텐츠는 {@code sessionId == null} 그룹으로 묶인다.
 */
public record LearningSessionGroup(
        Long sessionId,
        String sessionName,
        int seq,
        int avgProgress,
        List<LearningContentView> contents
) {
}
