package com.ssa.lms.content.service;

import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.content.repository.ProgressRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 과정 진도율/필수 이수 계산 검증 — 이수 트랙(P2-B)이 계약으로 읽는 값의 정확성.
 */
@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock ProgressRepository progressRepository;
    @Mock ContentRepository contentRepository;
    @Mock UserRepository userRepository;
    @Mock EnrollmentRepository enrollmentRepository;

    @InjectMocks ProgressService progressService;

    @Test
    @DisplayName("과정 진도율 = 활성 콘텐츠 진도율 평균 (기록 없는 콘텐츠는 0)")
    void courseProgressRatio() {
        // 활성 콘텐츠 4개, 진도율 합계 250 → 평균 62.5 → 반올림 63
        when(contentRepository.countByCourseIdAndStatus(1L, ContentStatus.ACTIVE)).thenReturn(4L);
        when(progressRepository.sumProgressRate(9L, 1L, ContentStatus.ACTIVE)).thenReturn(250L);

        assertThat(progressService.courseProgressRatio(9L, 1L)).isEqualTo(63);
    }

    @Test
    @DisplayName("활성 콘텐츠가 없으면 진도율은 0 (0으로 나누지 않음)")
    void courseProgressRatioNoContent() {
        when(contentRepository.countByCourseIdAndStatus(1L, ContentStatus.ACTIVE)).thenReturn(0L);
        assertThat(progressService.courseProgressRatio(9L, 1L)).isZero();
    }

    @Test
    @DisplayName("필수 콘텐츠를 모두 완료해야 이수(hasCompletedAllRequired)")
    void hasCompletedAllRequired() {
        lenient().when(progressRepository.countRequiredContents(1L, ContentStatus.ACTIVE)).thenReturn(3L);

        when(progressRepository.countCompletedRequired(9L, 1L, ContentStatus.ACTIVE)).thenReturn(2L);
        assertThat(progressService.hasCompletedAllRequired(9L, 1L)).isFalse();

        when(progressRepository.countCompletedRequired(9L, 1L, ContentStatus.ACTIVE)).thenReturn(3L);
        assertThat(progressService.hasCompletedAllRequired(9L, 1L)).isTrue();
    }

    @Test
    @DisplayName("필수 콘텐츠가 하나도 없으면 이수(false) — 아직 이수 대상 미구성")
    void hasCompletedAllRequiredNoRequired() {
        when(progressRepository.countRequiredContents(1L, ContentStatus.ACTIVE)).thenReturn(0L);
        assertThat(progressService.hasCompletedAllRequired(9L, 1L)).isFalse();
    }
}
