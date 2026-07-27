package com.ssa.lms.content.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 진도 계산 로직 단위 테스트 — 기존 프론트 JS 진도(재생 위치/도달 페이지)를 서버로 옮긴 규칙 검증.
 */
class ProgressTest {

    private static final int COMPLETION_RATE = 90;

    private Progress newProgress() {
        return Progress.builder().user(null).content(null).build();
    }

    @Test
    @DisplayName("동영상 진도율은 재생 위치/전체 길이 비율로 계산되고, 임계값 이상이면 완료 처리된다")
    void videoProgress() {
        Progress p = newProgress();

        p.updateVideoProgress(30, 100, COMPLETION_RATE);   // 30%
        assertThat(p.getProgressRate()).isEqualTo(30);
        assertThat(p.isCompleted()).isFalse();
        assertThat(p.getLastPositionSeconds()).isEqualTo(30);

        p.updateVideoProgress(95, 100, COMPLETION_RATE);   // 95% ≥ 90 → 완료
        assertThat(p.getProgressRate()).isEqualTo(95);
        assertThat(p.isCompleted()).isTrue();
        assertThat(p.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("진도율은 되돌아가도 하락하지 않는다(도달 최대치 유지), 재생 위치는 최신값으로 갱신")
    void videoProgressNeverDecreases() {
        Progress p = newProgress();
        p.updateVideoProgress(80, 100, COMPLETION_RATE);
        p.updateVideoProgress(10, 100, COMPLETION_RATE);   // 뒤로 감기

        assertThat(p.getProgressRate()).isEqualTo(80);      // 진도율 유지
        assertThat(p.getLastPositionSeconds()).isEqualTo(10); // 이어보기 위치는 최신
    }

    @Test
    @DisplayName("문서 진도율은 최대 도달 페이지/전체 페이지로 계산된다")
    void documentProgress() {
        Progress p = newProgress();

        p.updateDocumentProgress(5, 10, COMPLETION_RATE);   // 50%
        assertThat(p.getProgressRate()).isEqualTo(50);
        assertThat(p.getMaxPageReached()).isEqualTo(5);
        assertThat(p.isCompleted()).isFalse();

        p.updateDocumentProgress(3, 10, COMPLETION_RATE);   // 되돌아감 → 최대 유지
        assertThat(p.getMaxPageReached()).isEqualTo(5);

        p.updateDocumentProgress(10, 10, COMPLETION_RATE);  // 100% → 완료
        assertThat(p.getProgressRate()).isEqualTo(100);
        assertThat(p.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("전체 길이가 없으면 진도율은 계산하지 않고 재생 위치만 저장한다")
    void videoWithoutDurationKeepsPositionOnly() {
        Progress p = newProgress();
        p.updateVideoProgress(120, null, COMPLETION_RATE);

        assertThat(p.getLastPositionSeconds()).isEqualTo(120);
        assertThat(p.getProgressRate()).isZero();
        assertThat(p.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("수동 완료는 진도율을 100으로 올리고 완료 처리한다")
    void markCompleted() {
        Progress p = newProgress();
        p.markCompleted();

        assertThat(p.getProgressRate()).isEqualTo(100);
        assertThat(p.isCompleted()).isTrue();
        assertThat(p.getCompletedAt()).isNotNull();
    }
}
