package com.ssa.lms.content.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 콘텐츠별 학습 진도 — 훈련생 1명 × 콘텐츠 1개 당 1행.
 *
 * <p>기존 프론트 JS 진도 로직(동영상 재생 위치, 문서 최대 도달 페이지)을 서버에 영속화한 것이다.
 * 이어보기(마지막 위치) 와 이수 판정(진도율/완료)에 쓰인다. 3년 보존 대상이라 삭제하지 않는다.</p>
 */
@Entity
@Table(name = "content_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_progress_user_content", columnNames = {"user_id", "content_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Progress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    /** 진도율 0~100 */
    @Column(name = "progress_rate", nullable = false)
    private int progressRate;

    @Column(nullable = false)
    private boolean completed;

    /** VIDEO 마지막 재생 위치(초) — 이어보기 */
    @Column(name = "last_position_seconds")
    private Integer lastPositionSeconds;

    /** DOCUMENT 최대 도달 페이지 */
    @Column(name = "max_page_reached")
    private Integer maxPageReached;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_accessed_at", nullable = false)
    private LocalDateTime lastAccessedAt;

    @Builder
    private Progress(User user, Content content) {
        this.user = user;
        this.content = content;
        this.progressRate = 0;
        this.completed = false;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 동영상 진도 갱신. 진도율은 되돌아가도 최대치를 유지한다(도달 기준).
     *
     * @param positionSeconds 현재 재생 위치(초)
     * @param durationSeconds 전체 길이(초, 0/null 이면 진도율 계산 생략)
     * @param completionRate  완료로 간주할 진도율 임계값(예: 90)
     */
    public void updateVideoProgress(int positionSeconds, Integer durationSeconds, int completionRate) {
        this.lastPositionSeconds = positionSeconds;
        this.lastAccessedAt = LocalDateTime.now();
        if (durationSeconds != null && durationSeconds > 0) {
            int rate = (int) Math.min(100L, Math.round(positionSeconds * 100.0 / durationSeconds));
            raiseRate(rate, completionRate);
        }
    }

    /**
     * 문서 진도 갱신. 최대 도달 페이지 기준으로 진도율을 계산한다.
     *
     * @param pageReached  현재 도달 페이지
     * @param totalPages   전체 페이지 수(0/null 이면 진도율 계산 생략)
     * @param completionRate 완료로 간주할 진도율 임계값
     */
    public void updateDocumentProgress(int pageReached, Integer totalPages, int completionRate) {
        this.lastAccessedAt = LocalDateTime.now();
        if (this.maxPageReached == null || pageReached > this.maxPageReached) {
            this.maxPageReached = pageReached;
        }
        if (totalPages != null && totalPages > 0) {
            int rate = (int) Math.min(100L, Math.round(this.maxPageReached * 100.0 / totalPages));
            raiseRate(rate, completionRate);
        }
    }

    /** 진도율은 하락하지 않으며, 임계값 이상이면 완료 처리한다. */
    private void raiseRate(int rate, int completionRate) {
        if (rate > this.progressRate) {
            this.progressRate = rate;
        }
        if (!this.completed && this.progressRate >= completionRate) {
            this.completed = true;
            this.completedAt = LocalDateTime.now();
        }
    }

    /** 강제 완료 처리(수동 완료 버튼 등). */
    public void markCompleted() {
        this.progressRate = 100;
        this.lastAccessedAt = LocalDateTime.now();
        if (!this.completed) {
            this.completed = true;
            this.completedAt = LocalDateTime.now();
        }
    }
}
