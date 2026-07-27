package com.ssa.lms.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ProgressQueryService} 실제 구현체 (P2-A 제공, PARALLEL-P2.md 계약).
 *
 * <p>이 {@code @Service} 빈이 등록되면 base 의 fallback({@code ProgressContractConfig},
 * {@code @ConditionalOnMissingBean})은 자동으로 물러난다 — fallback 파일은 수정/삭제하지 않는다(머지 충돌 방지).</p>
 *
 * <p>이수 판정(P2-B)은 이 계약을 통해 A 도메인의 진도 데이터를 읽는다. 진도율/필수 이수 계산 로직은
 * {@link ProgressService} 에 중앙화되어 있어 훈련생 화면과 동일한 기준을 공유한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressQueryServiceImpl implements ProgressQueryService {

    private final ProgressService progressService;

    @Override
    public int completedRatio(Long userId, Long courseId) {
        return progressService.courseProgressRatio(userId, courseId);
    }

    @Override
    public boolean hasCompletedAll(Long userId, Long courseId) {
        return progressService.hasCompletedAllRequired(userId, courseId);
    }
}
